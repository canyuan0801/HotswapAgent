
package org.hotswap.agent.plugin.hibernate_jakarta;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.hibernate_jakarta.proxy.EntityManagerFactoryProxy;
import org.hotswap.agent.plugin.hibernate_jakarta.proxy.SessionFactoryProxy;



public class HibernateRefreshCommands {

    private static AgentLogger LOGGER = AgentLogger.getLogger(HibernateRefreshCommands.class);


    public static boolean reloadFlag = false;

    public static void reloadEntityManagerFactory() {
        LOGGER.debug("Refreshing hibernate configuration.");
        EntityManagerFactoryProxy.refreshProxiedFactories();
        LOGGER.reload("Hibernate EntityMangerFactory configuration refreshed.");
        reloadFlag = false;
    }

    public static void reloadSessionFactory() {
        LOGGER.debug("Refreshing SessionFactory configuration.");
        SessionFactoryProxy.refreshProxiedFactories();
        LOGGER.reload("Hibernate SessionFactory configuration refreshed.");
        reloadFlag = false;
    }
}
