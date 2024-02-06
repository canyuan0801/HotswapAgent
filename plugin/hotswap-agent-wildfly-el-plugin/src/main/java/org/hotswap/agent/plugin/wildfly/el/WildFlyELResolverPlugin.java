
package org.hotswap.agent.plugin.wildfly.el;

import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.Maven;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.annotation.Versions;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.command.Scheduler.DuplicateSheduleBehaviour;
import org.hotswap.agent.config.PluginConfiguration;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.PluginManagerInvoker;


@Plugin(name = "WildFlyELResolver",
        group = "groupELResolver",
        description = "Purge WildFlyELResolver class cache on any class redefinition.",
        testedVersions = { "1.0.5.Final" },
        expectedVersions = { "1.0.5.Final" })
@Versions(maven = { @Maven(value = "[1.0,)", artifactId = "jboss-el-api_3.0_spec", groupId = "org.jboss.spec.javax.el") })
public class WildFlyELResolverPlugin {

    private static AgentLogger LOGGER = AgentLogger.getLogger(WildFlyELResolverPlugin.class);

    public static final String PURGE_CLASS_CACHE_METHOD_NAME = "$$ha$resetCache";

    @Init
    Scheduler scheduler;

    @Init
    ClassLoader appClassLoader;


    @OnClassLoadEvent(classNameRegexp = "javax.el.BeanELResolver")
    public static void beanELResolverRegisterVariable(CtClass ctClass, ClassPool classPool) throws CannotCompileException, NotFoundException {
        for (CtConstructor constructor : ctClass.getDeclaredConstructors()) {
            constructor.insertAfter("java.lang.ClassLoader $$cl = Thread.currentThread().getContextClassLoader();" + PluginManagerInvoker.buildInitializePlugin(WildFlyELResolverPlugin.class, "$$cl"));
        }

        LOGGER.info("Patched JbossELResolver");
    }


    @OnClassLoadEvent(classNameRegexp = "org.jboss.el.cache.BeanPropertiesCache")
    public static void beanPropertiesCache(CtClass ctClass, ClassPool classPool) throws CannotCompileException, NotFoundException {
        for (CtConstructor constructor : ctClass.getDeclaredConstructors()) {
            constructor.insertAfter("java.lang.ClassLoader $$cl = Thread.currentThread().getContextClassLoader();" + PluginManagerInvoker.buildInitializePlugin(WildFlyELResolverPlugin.class, "$$cl"));
        }
        LOGGER.info("Patched org.jboss.el.cache.BeanPropertiesCache");
    }


    @OnClassLoadEvent(classNameRegexp = "org.jboss.el.cache.BeanPropertiesCache.SoftConcurrentHashMap")
    public static void beanPropertiesCacheSoftConcurrentHashMap(CtClass ctClass, ClassPool classPool) throws CannotCompileException, NotFoundException {
        ctClass.addMethod(CtMethod.make("public java.util.Set entrySet() { return map.entrySet();}", ctClass));
        LOGGER.info("Patched org.jboss.el.cache.BeanPropertiesCache$SoftConcurrentHashMap");
    }


    @OnClassLoadEvent(classNameRegexp = "org.jboss.el.cache.FactoryFinderCache")
    public static void factoryFinderCache(CtClass ctClass, ClassPool classPool) throws CannotCompileException, NotFoundException {
        for (CtConstructor constructor : ctClass.getDeclaredConstructors()) {
            constructor.insertAfter("java.lang.ClassLoader $$cl = Thread.currentThread().getContextClassLoader();" +
                    PluginManagerInvoker.buildInitializePlugin(WildFlyELResolverPlugin.class, "$$cl"));
        }
        LOGGER.info("Patched org.jboss.el.cache.FactoryFinderCache");
    }


    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE)
    public void invalidateClassCache(Class<?> original) throws Exception {
        LOGGER.trace("Running invalidateClassCache {}", appClassLoader);
        PurgeWildFlyBeanELResolverCacheCommand cmd = new PurgeWildFlyBeanELResolverCacheCommand(appClassLoader, original.getName());
        scheduler.scheduleCommand(cmd, 250, DuplicateSheduleBehaviour.SKIP);
    }


    @Init
    public void initializeInstance(PluginConfiguration pluginConfiguration) {
        LOGGER.info("WildFlyELResolver plugin initialized");
    }
}