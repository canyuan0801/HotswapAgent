
package org.hotswap.agent.plugin.hotswapper;

import org.hotswap.agent.logging.AgentLogger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class HotswapperCommand {
    private static AgentLogger LOGGER = AgentLogger.getLogger(HotswapperCommand.class);




    private static HotSwapperJpda hotSwapper = null;

    public static synchronized void hotswap(String port, final HashMap<Class<?>, byte[]> reloadMap) {

        synchronized (reloadMap) {
            if (hotSwapper == null) {
                LOGGER.debug("Starting HotSwapperJpda agent on JPDA transport socket - port {}, classloader {}", port, HotswapperCommand.class.getClassLoader());
                try {
                    hotSwapper = new HotSwapperJpda(port);
                } catch (IOException e) {
                    LOGGER.error("Unable to connect to debug session. Did you start the application with debug enabled " +
                            "(i.e. java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000)", e);
                } catch (Exception e) {
                    LOGGER.error("Unable to connect to debug session. Please check port property setting '{}'.", e, port);
                }
            }

            if (hotSwapper != null) {
                LOGGER.debug("Reloading classes {}", Arrays.toString(reloadMap.keySet().toArray()));



                Map<String, byte[]> reloadMapClassNames = new HashMap<>();
                for (Map.Entry<Class<?>, byte[]> entry : reloadMap.entrySet()) {
                    reloadMapClassNames.put(entry.getKey().getName(), entry.getValue());
                }

                hotSwapper.reload(reloadMapClassNames);

                reloadMap.clear();
                LOGGER.debug("HotSwapperJpda agent reload complete.");
            }
        }
    }
}
