
package org.hotswap.agent.plugin.hibernate_jakarta.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.hibernate.Version;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.ReflectionHelper;


public class EntityManagerFactoryProxy {
    private static AgentLogger LOGGER = AgentLogger.getLogger(EntityManagerFactoryProxy.class);

    private static Map<String, EntityManagerFactoryProxy> proxiedFactories = new HashMap<>();

    final Object reloadLock = new Object();

    EntityManagerFactory currentInstance;

    String persistenceUnitName;
    PersistenceUnitInfo info;
    Map properties;


    Object builder;


    public static EntityManagerFactoryProxy getWrapper(String persistenceUnitName) {
        if (!proxiedFactories.containsKey(persistenceUnitName)) {
            proxiedFactories.put(persistenceUnitName, new EntityManagerFactoryProxy());
        }
        return proxiedFactories.get(persistenceUnitName);
    }


    public static void refreshProxiedFactories() {
        String[] version = Version.getVersionString().split("\\.");
        boolean version43OrGreater = false;
        try {
            version43OrGreater = Integer.valueOf(version[0]) >= 5 || (Integer.valueOf(version[0]) == 4 && Integer.valueOf(version[1]) >= 3);
        } catch (Exception e) {
            LOGGER.warning("Unable to resolve hibernate version '{}'", version);
        }

        for (EntityManagerFactoryProxy wrapper : proxiedFactories.values()) {
            String persistenceClassName = wrapper.properties == null ? null :
                    (String) wrapper.properties.get("PERSISTENCE_CLASS_NAME");

            try {

                synchronized (wrapper.reloadLock) {
                    if ("org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider".equals(persistenceClassName)) {
                        wrapper.refreshProxiedFactorySpring();
                    } else if (version43OrGreater) {
                        wrapper.refreshProxiedFactoryVersion43OrGreater();
                    } else {
                        wrapper.refreshProxiedFactory();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void refreshProxiedFactorySpring() {
        try {
            currentInstance = (EntityManagerFactory) ReflectionHelper.invoke(builder, builder.getClass(),
                    "createContainerEntityManagerFactory",
                    new Class[]{PersistenceUnitInfo.class, Map.class}, info, properties);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Unable to reload persistence unit {}", info, e);
        }
    }

    public void refreshProxiedFactoryVersion43OrGreater() {
        if (info == null) {
            currentInstance = Persistence.createEntityManagerFactory(persistenceUnitName, properties);
        } else {
            try {
                Class bootstrapClazz = loadClass("org.hibernate.jpa.boot.spi.Bootstrap");
                Class builderClazz = loadClass("org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder");

                Object builder = ReflectionHelper.invoke(null, bootstrapClazz, "getEntityManagerFactoryBuilder",
                        new Class[]{PersistenceUnitInfo.class, Map.class}, info, properties);

                currentInstance = (EntityManagerFactory) ReflectionHelper.invoke(builder, builderClazz, "build",
                        new Class[]{});
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("Unable to reload persistence unit {}", info, e);
            }
        }
    }


    public void refreshProxiedFactory() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {

        try {
            Class entityManagerFactoryRegistryClazz = loadClass("org.hibernate.ejb.internal.EntityManagerFactoryRegistry");
            Object instance = ReflectionHelper.get(null, entityManagerFactoryRegistryClazz, "INSTANCE");
            ReflectionHelper.invoke(instance, entityManagerFactoryRegistryClazz, "removeEntityManagerFactory",
                    new Class[] {String.class, EntityManagerFactory.class}, persistenceUnitName, currentInstance);
        } catch (Exception e) {
            LOGGER.error("Unable to clear previous instance of entity manager factory");
        }


        buildFreshEntityManagerFactory();
    }



    private void buildFreshEntityManagerFactory() {
        try {
            Class ejb3ConfigurationClazz = loadClass("org.hibernate.ejb.Ejb3Configuration");
            LOGGER.trace("new Ejb3Configuration()");
            Object cfg = ejb3ConfigurationClazz.newInstance();

            LOGGER.trace("cfg.configure( info, properties );");

            if (info != null) {
                ReflectionHelper.invoke(cfg, ejb3ConfigurationClazz, "configure",
                        new Class[]{PersistenceUnitInfo.class, Map.class}, info, properties);
            }
            else {
                ReflectionHelper.invoke(cfg, ejb3ConfigurationClazz, "configure",
                        new Class[]{String.class, Map.class}, persistenceUnitName, properties);
            }

            LOGGER.trace("configured.buildEntityManagerFactory()");
            currentInstance = (EntityManagerFactory) ReflectionHelper.invoke(cfg, ejb3ConfigurationClazz, "buildEntityManagerFactory",
                    new Class[]{});


        } catch (Exception e) {
            LOGGER.error("Unable to build fresh entity manager factory for persistence unit {}", persistenceUnitName);
        }
    }


    public EntityManagerFactory proxy(Object builder, EntityManagerFactory factory, String persistenceUnitName,
                                      PersistenceUnitInfo info, Map properties) {
        this.builder = builder;
        this.currentInstance = factory;
        this.persistenceUnitName = persistenceUnitName;
        this.info = info;
        this.properties = properties;

        return (EntityManagerFactory) Proxy.newProxyInstance(
                currentInstance.getClass().getClassLoader(), currentInstance.getClass().getInterfaces(),
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        synchronized (reloadLock) {}

                        return method.invoke(currentInstance, args);
                    }
                });
    }

    private Class loadClass(String name) throws ClassNotFoundException {
        return getClass().getClassLoader().loadClass(name);
    }
}
