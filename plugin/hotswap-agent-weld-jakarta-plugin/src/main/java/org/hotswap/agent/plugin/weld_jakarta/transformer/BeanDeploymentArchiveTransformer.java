
package org.hotswap.agent.plugin.weld_jakarta.transformer;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.cdi.HaCdiCommons;
import org.hotswap.agent.plugin.weld_jakarta.WeldJakartaPlugin;
import org.hotswap.agent.util.PluginManagerInvoker;


public class BeanDeploymentArchiveTransformer {

    private static AgentLogger LOGGER = AgentLogger.getLogger(BeanDeploymentArchiveTransformer.class);

    
    @OnClassLoadEvent(classNameRegexp = "org.jboss.weld.environment.deployment.WeldBeanDeploymentArchive")
    public static void transform(ClassPool classPool, CtClass clazz) throws NotFoundException, CannotCompileException {
        if (!HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        StringBuilder src = new StringBuilder("{");
        src.append(PluginManagerInvoker.buildInitializePlugin(WeldJakartaPlugin.class));
        src.append(PluginManagerInvoker.buildCallPluginMethod(WeldJakartaPlugin.class, "init"));
        src.append("org.hotswap.agent.plugin.weld.command.BeanClassRefreshAgent.registerArchive(getClass().getClassLoader(), this, null);");
        src.append("}");

        for (CtConstructor constructor : clazz.getDeclaredConstructors()) {
            constructor.insertAfter(src.toString());
        }

        LOGGER.debug("Class '{}' patched with BDA registration.", clazz.getName());
    }

    
    @OnClassLoadEvent(classNameRegexp = "org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl")
    public static void transformJbossBda(ClassPool classPool, CtClass clazz) throws NotFoundException, CannotCompileException {
        if (!HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        StringBuilder src = new StringBuilder("{");
        src.append("if (beansXml!=null && beanArchiveType!=null && (\"EXPLICIT\".equals(beanArchiveType.toString()) || \"IMPLICIT\".equals(beanArchiveType.toString()))){");
        src.append(PluginManagerInvoker.buildInitializePlugin(WeldJakartaPlugin.class, "module.getClassLoader()"));
        src.append(PluginManagerInvoker.buildCallPluginMethod("module.getClassLoader()", WeldJakartaPlugin.class, "initInJBossAS"));
        src.append("    Class agC = Class.forName(\"org.hotswap.agent.plugin.weld.command.BeanClassRefreshAgent\", true, module.getClassLoader());");
        src.append("    java.lang.reflect.Method agM  = agC.getDeclaredMethod(\"registerArchive\", new Class[] {java.lang.ClassLoader.class, org.jboss.weld.bootstrap.spi.BeanDeploymentArchive.class, java.lang.String.class});");
        src.append("    agM.invoke(null, new Object[] { module.getClassLoader(),this, beanArchiveType.toString()});");
        src.append("}}");

        for (CtConstructor constructor : clazz.getDeclaredConstructors()) {
            constructor.insertAfter(src.toString());
        }

        LOGGER.debug("Class 'org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl' patched with BDA registration.");
    }

    
    @OnClassLoadEvent(classNameRegexp = "org.glassfish.weld.BeanDeploymentArchiveImpl")
    public static void transformGlassFishBda(ClassPool classPool, CtClass clazz) throws NotFoundException, CannotCompileException {
        if (!HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        StringBuilder src = new StringBuilder("{");
        src.append(PluginManagerInvoker.buildInitializePlugin(WeldJakartaPlugin.class, "this.moduleClassLoaderForBDA"));
        src.append(PluginManagerInvoker.buildCallPluginMethod("this.moduleClassLoaderForBDA", WeldJakartaPlugin.class, "initInGlassFish"));
        src.append("    Class agC = Class.forName(\"org.hotswap.agent.plugin.weld.command.BeanClassRefreshAgent\", true, this.moduleClassLoaderForBDA);");
        src.append("    java.lang.reflect.Method agM  = agC.getDeclaredMethod(\"registerArchive\", new Class[] {java.lang.ClassLoader.class, org.jboss.weld.bootstrap.spi.BeanDeploymentArchive.class, java.lang.String.class});");
        src.append("    agM.invoke(null, new Object[] { this.moduleClassLoaderForBDA, this, null});");
        src.append("}");

        for (CtConstructor constructor : clazz.getDeclaredConstructors()) {
            constructor.insertAfter(src.toString());
        }

        LOGGER.debug("Class 'org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl' patched with BDA registration.");
    }
}
