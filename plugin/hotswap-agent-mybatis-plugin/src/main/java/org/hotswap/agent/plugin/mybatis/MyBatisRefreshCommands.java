
package org.hotswap.agent.plugin.mybatis;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.mybatis.proxy.ConfigurationProxy;
import org.hotswap.agent.plugin.mybatis.proxy.SpringMybatisConfigurationProxy;



public class MyBatisRefreshCommands {
    private static AgentLogger LOGGER = AgentLogger.getLogger(MyBatisRefreshCommands.class);


    public static boolean reloadFlag = false;

    public static void reloadConfiguration() {
        LOGGER.debug("Refreshing MyBatis configuration.");
        ConfigurationProxy.refreshProxiedConfigurations();
        SpringMybatisConfigurationProxy.refreshProxiedConfigurations();
        LOGGER.reload("MyBatis configuration refreshed.");
        reloadFlag = false;
    }
}
