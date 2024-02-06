
package org.hotswap.agent.plugin.undertow;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.PluginManagerInvoker;


public class UndertowTransformer {

    private static AgentLogger LOGGER = AgentLogger.getLogger(UndertowTransformer.class);

    @OnClassLoadEvent(classNameRegexp = "io.undertow.servlet.core.DeploymentManagerImpl")
    public static void patchWebappLoader(CtClass ctClass) throws NotFoundException, CannotCompileException, ClassNotFoundException {

        try {
            ctClass.getDeclaredMethod("deploy").insertBefore( "{" +
                    "org.hotswap.agent.plugin.undertow.PrefixingResourceManager rm=" +
                        "new org.hotswap.agent.plugin.undertow.PrefixingResourceManager(originalDeployment.getResourceManager());" +
                    "originalDeployment.setResourceManager(rm);" +
                    UndertowPlugin.class.getName() + ".init(originalDeployment.getClassLoader(),rm);" +
                "}"
            );
        } catch (NotFoundException e) {
            LOGGER.error("io.undertow.servlet.core.DeploymentManagerImpl does not contain start() method.");
        }

        try {
            ctClass.getDeclaredMethod("stop").insertBefore(
                    PluginManagerInvoker.buildCallCloseClassLoader("originalDeployment.getClassLoader()") +
                    UndertowPlugin.class.getName() + ".close(originalDeployment.getClassLoader());"
            );
        } catch (NotFoundException e) {
            LOGGER.error("orgio.undertow.servlet.core.DeploymentManagerImpl does not contain stop() method.");
        }

    }
}
