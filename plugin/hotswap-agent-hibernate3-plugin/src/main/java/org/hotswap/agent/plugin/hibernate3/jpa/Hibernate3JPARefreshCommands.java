
package org.hotswap.agent.plugin.hibernate3.jpa;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.hibernate3.jpa.proxy.EntityManagerFactoryProxy;



public class Hibernate3JPARefreshCommands {


    private static AgentLogger LOGGER = AgentLogger.getLogger(Hibernate3JPARefreshCommands.class);


    public static boolean reloadFlag = false;


    public static void reloadEntityManagerFactory() {
        LOGGER.debug("Refreshing hibernate configuration.");
        EntityManagerFactoryProxy.refreshProxiedFactories();
        LOGGER.reload("Hibernate EntityMangerFactory configuration refreshed.");
        reloadFlag = false;
    }
}
