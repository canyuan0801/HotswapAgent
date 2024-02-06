
package org.hotswap.agent.plugin.deltaspike.transformer;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.javassist.*;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.cdi.HaCdiCommons;
import org.hotswap.agent.plugin.deltaspike.DeltaSpikePlugin;
import org.hotswap.agent.util.PluginManagerInvoker;


public class PartialBeanTransformer {

    private static AgentLogger LOGGER = AgentLogger.getLogger(PartialBeanTransformer.class);


    @OnClassLoadEvent(classNameRegexp = "org.apache.deltaspike.partialbean.impl.PartialBeanBindingExtension")
    public static void patchPartialBeanBindingExtension(ClassPool classPool, CtClass ctClass)  throws NotFoundException, CannotCompileException {
        if (HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        CtMethod init = ctClass.getDeclaredMethod("init");
        init.insertAfter(PluginManagerInvoker.buildInitializePlugin(DeltaSpikePlugin.class));
        LOGGER.debug("org.apache.deltaspike.partialbean.impl.PartialBeanBindingExtension enhanced with plugin initialization.");

        CtMethod createPartialBeanMethod = ctClass.getDeclaredMethod("createPartialBean");
        createPartialBeanMethod.insertAfter(
            "if (" + PluginManager.class.getName() + ".getInstance().isPluginInitialized(\"" + DeltaSpikePlugin.class.getName() + "\", beanClass.getClassLoader())) {" +
                PluginManagerInvoker.buildCallPluginMethod(DeltaSpikePlugin.class, "registerPartialBean",
                        "$_", "java.lang.Object",
                        "beanClass", "java.lang.Class"
                        ) +
            "}" +
            "return $_;"
        );
        LOGGER.debug("org.apache.deltaspike.partialbean.impl.PartialBeanBindingExtension patched.");
    }

}
