
package org.hotswap.agent.plugin.jersey2;

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

@Plugin(name = "Jersey2",
        description = "Jersey2 framework plugin - this does not handle HK2 changes",
        testedVersions = {"2.10.1"},
        expectedVersions = {"2.10.1"})
public class Jersey2Plugin {
    private static AgentLogger LOGGER = AgentLogger.getLogger(Jersey2Plugin.class);

    @Init
    Scheduler scheduler;

    @Init
    ClassLoader appClassLoader;

    Set<Object> registeredJerseyContainers = Collections.newSetFromMap(new WeakHashMap<Object, Boolean>());

    Set<Class<?>> allRegisteredClasses = Collections.newSetFromMap(new WeakHashMap<Class<?>, Boolean>());


    @OnClassLoadEvent(classNameRegexp = "org.glassfish.jersey.servlet.ServletContainer")
    public static void jerseyServletCallInitialized(CtClass ctClass, ClassPool classPool) throws NotFoundException, CannotCompileException {
        CtMethod init = ctClass.getDeclaredMethod("init", new CtClass[] { classPool.get("org.glassfish.jersey.servlet.WebConfig") });
        init.insertBefore(PluginManagerInvoker.buildInitializePlugin(Jersey2Plugin.class));
        LOGGER.info("org.glassfish.jersey.servlet.ServletContainer enhanced with plugin initialization.");

        String registerThis = PluginManagerInvoker.buildCallPluginMethod(Jersey2Plugin.class, "registerJerseyContainer", "this",
                "java.lang.Object", "getConfiguration()", "java.lang.Object");
        init.insertAfter(registerThis);


        CtMethod reload = ctClass.getDeclaredMethod("reload", new CtClass[] { classPool.get("org.glassfish.jersey.server.ResourceConfig") });
        reload.insertBefore("$1 = new org.glassfish.jersey.server.ResourceConfig($1);");
    }


    @OnClassLoadEvent(classNameRegexp = "org.glassfish.jersey.server.internal.scanning.AnnotationAcceptingListener")
    public static void fixAnnoationAcceptingListener(CtClass ctClass) throws NotFoundException, CannotCompileException {
        CtMethod process = ctClass.getDeclaredMethod("process");
        process.insertAfter("try { $2.close(); } catch (Exception e) {}");
    }


    @OnClassLoadEvent(classNameRegexp = "org.glassfish.jersey.ext.cdi1x.internal.SingleHk2LocatorManager")
    public static void fixSingleHk2LocatorManager(CtClass ctClass) throws NotFoundException, CannotCompileException {
        CtMethod process = ctClass.getDeclaredMethod("registerLocator");
        process.insertBefore("if (this.locator != null) return;");
        LOGGER.debug("SingleHk2LocatorManager : patched()");
    }


    public void registerJerseyContainer(Object jerseyContainer, Object resourceConfig) {
        try {
            Class<?> resourceConfigClass = resolveClass("org.glassfish.jersey.server.ResourceConfig");

            LOGGER.info("Jersey2Plugin - registerJerseyContainer : " + jerseyContainer.getClass().getName());

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

        Method scanClassesMethod = resourceConfigClass.getDeclaredMethod("scanClasses");
        scanClassesMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Class<?>> scannedClasses = (Set<Class<?>>) scanClassesMethod.invoke(resourceConfig);

        Method getRegisteredClassesMethod = resourceConfigClass.getDeclaredMethod("getRegisteredClasses");
        getRegisteredClassesMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Class<?>> registeredClasses = (Set<Class<?>>)getRegisteredClassesMethod.invoke(resourceConfig);

        Set<Class<?>> containerClasses = Collections.newSetFromMap(new WeakHashMap<Class<?>, Boolean>());
        containerClasses.addAll(scannedClasses);
        containerClasses.addAll(registeredClasses);
        return containerClasses;
    }


    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE)
    public void invalidate(CtClass ctClass, Class original) throws Exception {
        boolean reloaded = false;
        if (allRegisteredClasses.contains(original)) {
            scheduler.scheduleCommand(reloadJerseyContainers);
            reloaded = true;
        } else {




            if (AnnotationHelper.hasAnnotation(original, "javax.ws.rs.Path")
                    || AnnotationHelper.hasAnnotation(ctClass, "javax.ws.rs.Path")) {
                allRegisteredClasses.add(original);
                scheduler.scheduleCommand(reloadJerseyContainers);
                reloaded = true;
            }

        }
        if (!reloaded) {

            if (AnnotationHelper.hasAnnotation(original, "org.jvnet.hk2.annotations.Service")
                    || AnnotationHelper.hasAnnotation(ctClass, "org.jvnet.hk2.annotations.Service")) {

                scheduler.scheduleCommand(reloadJerseyContainers);


            }
        }
    }


    private Command reloadJerseyContainers = new Command() {
        public void executeCommand() {
            try {
                Class<?> containerClass = resolveClass("org.glassfish.jersey.server.spi.Container");
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

