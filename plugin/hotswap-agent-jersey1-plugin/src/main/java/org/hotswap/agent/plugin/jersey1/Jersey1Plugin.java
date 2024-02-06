
package org.hotswap.agent.plugin.jersey1;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.command.Command;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.AnnotationHelper;
import org.hotswap.agent.util.PluginManagerInvoker;
import org.hotswap.agent.util.ReflectionHelper;

@Plugin(name = "Jersey1",
        description = "Jersey1 framework plugin - this does not handle HK2 changes",
        testedVersions = {"1.18.3"},
        expectedVersions = {"1.x"})
public class Jersey1Plugin {
    private static AgentLogger LOGGER = AgentLogger.getLogger(Jersey1Plugin.class);

    @Init
    Scheduler scheduler;

    @Init
    ClassLoader appClassLoader;

    Set<Object> registeredJerseyContainers = Collections.newSetFromMap(new WeakHashMap<Object, Boolean>());
    Set<Class<?>> allRegisteredClasses = Collections.newSetFromMap(new WeakHashMap<Class<?>, Boolean>());


    @OnClassLoadEvent(classNameRegexp = "com.sun.jersey.spi.container.servlet.ServletContainer")
    public static void jerseyServletCallInitialized(CtClass ctClass, ClassPool classPool) throws NotFoundException, CannotCompileException {
        CtMethod init = ctClass.getDeclaredMethod("init", new CtClass[] { classPool.get("com.sun.jersey.spi.container.servlet.WebConfig") });
        init.insertBefore(PluginManagerInvoker.buildInitializePlugin(Jersey1Plugin.class));
        LOGGER.info("com.sun.jersey.spi.container.servlet.WebConfig enhanced with plugin initialization.");

        String registerThis = PluginManagerInvoker.buildCallPluginMethod(Jersey1Plugin.class, "registerJerseyContainer", "this",
                "java.lang.Object", "this.webComponent.getResourceConfig()", "java.lang.Object");
        init.insertAfter(registerThis);
    }


    public void registerJerseyContainer(Object jerseyContainer, Object resourceConfig) {
        try {
            Class<?> resourceConfigClass = resolveClass("com.sun.jersey.api.core.ResourceConfig");

            LOGGER.info("Jersey1 plugin - registerJerseyContainer : " + jerseyContainer.getClass().getName());

            Set<Class<?>> containerClasses = getContainerClasses(resourceConfigClass, resourceConfig);

            registeredJerseyContainers.add(jerseyContainer);
            allRegisteredClasses.addAll(containerClasses);

            LOGGER.debug("registerJerseyContainer : finished");
        } catch (Exception e) {
            LOGGER.error("Error registering Jersey Container.", e);
        }
    }


    private Set<Class<?>> getContainerClasses(Class<?> resourceConfigClass, Object resourceConfig)
                throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        Set<Class<?>> containerClasses = Collections.newSetFromMap(new WeakHashMap<Class<?>, Boolean>());

        Set<Class<?>> providerClasses = (Set<Class<?>>) ReflectionHelper.invoke(resourceConfig, resourceConfigClass, "getProviderClasses",  new Class[]{});
        if (providerClasses != null) {
            containerClasses.addAll(providerClasses);
        }

        Set<Class<?>> rootResourceClasses = (Set<Class<?>>) ReflectionHelper.invoke(resourceConfig, resourceConfigClass, "getRootResourceClasses",  new Class[]{});
        if (rootResourceClasses != null) {
            containerClasses.addAll(rootResourceClasses);
        }

        return containerClasses;
    }


    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE)
    public void invalidate(CtClass ctClass, Class original) throws Exception {
        if (allRegisteredClasses.contains(original)) {
            scheduler.scheduleCommand(reloadJerseyContainers);
        } else {





            if (AnnotationHelper.hasAnnotation(original, "javax.ws.rs.Path")
                    || AnnotationHelper.hasAnnotation(ctClass, "javax.ws.rs.Path")) {
                allRegisteredClasses.add(original);
                scheduler.scheduleCommand(reloadJerseyContainers);
            }
        }
    }


    private Command reloadJerseyContainers = new Command() {
        public void executeCommand() {
            try {
                Class<?> containerClass = resolveClass("com.sun.jersey.spi.container.servlet.ServletContainer");
                Method reloadMethod = containerClass.getDeclaredMethod("reload");

                for (Object jerseyContainer : registeredJerseyContainers) {
                    reloadMethod.invoke(jerseyContainer);
                }
                LOGGER.info("Reloaded Jersey Containers");
            } catch (Exception e) {
                LOGGER.error("Error reloading Jersey Container.", e);
            }
        }
    };

    private Class<?> resolveClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, appClassLoader);
    }

}

