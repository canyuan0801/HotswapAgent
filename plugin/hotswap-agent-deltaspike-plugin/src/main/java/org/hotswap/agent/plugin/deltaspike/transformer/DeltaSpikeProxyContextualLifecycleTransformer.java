
package org.hotswap.agent.plugin.deltaspike.transformer;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.*;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.cdi.HaCdiCommons;
import org.hotswap.agent.plugin.deltaspike.DeltaSpikePlugin;
import org.hotswap.agent.util.PluginManagerInvoker;


public class DeltaSpikeProxyContextualLifecycleTransformer {

    private static AgentLogger LOGGER = AgentLogger.getLogger(DeltaSpikeProxyContextualLifecycleTransformer.class);


    @OnClassLoadEvent(classNameRegexp = "org.apache.deltaspike.proxy.api.DeltaSpikeProxyContextualLifecycle")
    public static void patchDeltaSpikeProxyContextualLifecycle(ClassPool classPool, CtClass ctClass) throws CannotCompileException, NotFoundException {
        if (HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        CtMethod methodCreate = ctClass.getDeclaredMethod("create");
        methodCreate.insertAfter(
                "{" +
                    PluginManagerInvoker.buildInitializePlugin(DeltaSpikePlugin.class) +
                    PluginManagerInvoker.buildCallPluginMethod(DeltaSpikePlugin.class, "registerRepoProxy", "$_", "java.lang.Object", "this.targetClass", "java.lang.Class")+
                "}" +
                "return $_;"
        );

        LOGGER.debug("org.apache.deltaspike.proxy.api.DeltaSpikeProxyContextualLifecycle - registration hook added.");
    }

}
