
package org.hotswap.agent.plugin.hibernate;

import org.hibernate.Version;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.hibernate.proxy.EntityManagerFactoryProxy;
import org.hotswap.agent.util.PluginManagerInvoker;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class HibernatePersistenceHelper {
    private static AgentLogger LOGGER = AgentLogger.getLogger(HibernatePersistenceHelper.class);


    static Set<String> wrappedPersistenceUnitNames = new HashSet<>();


    public static EntityManagerFactory createContainerEntityManagerFactoryProxy(Object builder, PersistenceUnitInfo info, Map properties,
                                                                                EntityManagerFactory original) {

        if (wrappedPersistenceUnitNames.contains(info.getPersistenceUnitName())) {
            return original;
        }
        wrappedPersistenceUnitNames.add(info.getPersistenceUnitName());

        EntityManagerFactoryProxy wrapper = EntityManagerFactoryProxy.getWrapper(info.getPersistenceUnitName());
        EntityManagerFactory proxy = wrapper.proxy(builder, original, info.getPersistenceUnitName(), info, properties);

        initPlugin(original);

        LOGGER.debug("Returning container EntityManager proxy {} instead of EntityManager {}", proxy.getClass(), original);
        return proxy;
    }


    public static EntityManagerFactory createEntityManagerFactoryProxy(Object builder, String persistenceUnitName, Map properties,
                                                                       EntityManagerFactory original) {

        if (wrappedPersistenceUnitNames.contains(persistenceUnitName)) {
            return original;
        }
        wrappedPersistenceUnitNames.add(persistenceUnitName);

        EntityManagerFactoryProxy wrapper = EntityManagerFactoryProxy.getWrapper(persistenceUnitName);
        EntityManagerFactory proxy = wrapper.proxy(builder, original, persistenceUnitName, null, properties);

        initPlugin(original);

        LOGGER.debug("Returning EntityManager proxy {} instead of EntityManager {}", proxy.getClass(), original);
        return proxy;
    }


    private static void initPlugin(EntityManagerFactory original) {
        ClassLoader appClassLoader = original.getClass().getClassLoader();

        String version = Version.getVersionString();

        PluginManagerInvoker.callInitializePlugin(HibernatePlugin.class, appClassLoader);
        PluginManagerInvoker.callPluginMethod(HibernatePlugin.class, appClassLoader,
                "init",
                new Class[]{String.class, Boolean.class},
                new Object[]{version, true});

    }
}
