
package org.hotswap.agent.plugin.owb_jakarta.transformer;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.cdi.HaCdiCommons;
import org.hotswap.agent.plugin.owb_jakarta.OwbJakartaPlugin;
import org.hotswap.agent.util.PluginManagerInvoker;


public class BeansDeployerTransformer {

    private static AgentLogger LOGGER = AgentLogger.getLogger(BeansDeployerTransformer.class);


    @OnClassLoadEvent(classNameRegexp = "org.apache.webbeans.config.BeansDeployer")
    public static void transform(CtClass clazz, ClassPool classPool) throws NotFoundException, CannotCompileException {
        if (!HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        StringBuilder src = new StringBuilder(" if (deployed) {");
        src.append("ClassLoader curCl = Thread.currentThread().getContextClassLoader();");
        src.append(PluginManagerInvoker.buildInitializePlugin(OwbJakartaPlugin.class, "curCl"));
        src.append(PluginManagerInvoker.buildCallPluginMethod("curCl", OwbJakartaPlugin.class, "init"));
        src.append(PluginManagerInvoker.buildCallPluginMethod("curCl", OwbJakartaPlugin.class, "registerBeansXmls", "$1.getBeanXmls()", "java.util.Set"));
        src.append("}");

        CtMethod startApplication = clazz.getDeclaredMethod("deploy");
        startApplication.insertAfter(src.toString());

        LOGGER.debug("Class '{}' patched with OwbPlugin registration.", clazz.getName());
    }

}
