
package org.hotswap.agent.plugin.hibernate3.session;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.hibernate3.session.proxy.SessionFactoryProxy;


public class Hibernate3RefreshCommand {


    private static AgentLogger LOGGER = AgentLogger.getLogger(Hibernate3RefreshCommand.class);


    public static boolean reloadFlag = false;


    public static void reloadSessionFactory() {
        LOGGER.debug("Refreshing SessionFactory configuration.");
        SessionFactoryProxy.refreshProxiedFactories();
        LOGGER.reload("Hibernate SessionFactory configuration refreshed.");
        reloadFlag = false;
    }
}
