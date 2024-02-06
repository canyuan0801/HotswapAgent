
package org.hotswap.agent.plugin.tomcat;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;

import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.PluginManagerInvoker;


public class TomcatSetup {
    private static AgentLogger LOGGER = AgentLogger.getLogger(TomcatSetup.class);

    public TomcatSetup(ClassLoader originalClassLoader) {

        System.err.println("PluginManagerInstance = " + PluginManager.getInstance());
        PluginManagerInvoker.callInitializePlugin(TomcatPlugin.class, originalClassLoader);


        URL[] extraClassPath = (URL[]) PluginManagerInvoker.callPluginMethod(TomcatPlugin.class, originalClassLoader,
                "getExtraClassPath", new Class[] {}, new Object[] {});
        System.err.println("extraClassPath =  " + Arrays.toString(extraClassPath));
        LOGGER.debug("extraClassPath = {}", extraClassPath);


        if (extraClassPath.length > 0) {
            LOGGER.debug("Registering extraClasspath {} to classloader {}", extraClassPath, originalClassLoader);
            for (URL url : extraClassPath) {

                try {
                    File classRepository = new File(url.toURI());

                    Method m = originalClassLoader.getClass().getDeclaredMethod("addRepository", String.class, File.class);
                    m.setAccessible(true);
                    m.invoke(originalClassLoader, classRepository.getAbsolutePath() + "/", classRepository);
                } catch (Exception e) {
                    throw new Error(e);
                }

            }
        }
    }


}
