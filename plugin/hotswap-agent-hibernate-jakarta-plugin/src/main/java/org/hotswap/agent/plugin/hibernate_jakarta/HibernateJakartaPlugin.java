
package org.hotswap.agent.plugin.hibernate_jakarta;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.hotswap.agent.annotation.*;
import org.hotswap.agent.command.Command;
import org.hotswap.agent.command.ReflectionCommand;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.AnnotationHelper;


@Plugin(name = "HibernateJakarta",
        group = "groupHibernate",
        fallback = true,
        description = "Reload Hibernate configuration after entity create/change.",
        testedVersions = {"All between 5.5.- - 6.0.0"},
        expectedVersions = {"5.5.x", "5.6.0", "6.0.0" },
        supportClass = { HibernateTransformers.class})
@Versions(
        maven = {
            @Maven(value = "[5.5,6.0)", artifactId = "hibernate-core-jakarta", groupId = "org.hibernate"),
        },
        manifest= {
                @Manifest(value="[5.5,6.0)", names= {
                        @Name(key=Name.BundleSymbolicName, value="org.hibernate.validator")
                }),
                @Manifest(value="[5.5,6.0)", names= {
                        @Name(key=Name.BundleSymbolicName, value="org.hibernate.entitymanager")
                }),
                @Manifest(value="[5.5,6.0)", names= {
                        @Name(key=Name.BundleSymbolicName, value="org.hibernate.core")
                }),
                @Manifest(value="[5.5,6.0)", names= {
                        @Name(key=Name.ImplementationUrl, value="http:
                        @Name(key=Name.ImplementationVendorId, value="org.hibernate")
                }),
        }
        )
public class HibernateJakartaPlugin
{
    private static final String ENTITY_ANNOTATION = "jakarta.persistence.Entity";
    private static AgentLogger LOGGER = AgentLogger.getLogger(HibernateJakartaPlugin.class);

    @Init
    Scheduler scheduler;

    @Init
    ClassLoader appClassLoader;

    Set<Object> regAnnotatedMetaDataProviders = Collections.newSetFromMap(new WeakHashMap<Object, Boolean>());

    Map<Object, String> regBeanMetaDataManagersMap = new WeakHashMap<Object, String>();


    Command reloadEntityManagerFactoryCommand =
            new ReflectionCommand(this, HibernateRefreshCommands.class.getName(), "reloadEntityManagerFactory");
    Command reloadSessionFactoryCommand =
            new ReflectionCommand(this, HibernateRefreshCommands.class.getName(), "reloadSessionFactory");

    private Command invalidateHibernateValidatorCaches = new Command() {
        @Override
        public void executeCommand() {
            LOGGER.debug("Refreshing BeanMetaDataManagerCache/AnnotatedMetaDataProvider cache.");

            try {
                Method resetCacheMethod1 = resolveClass("org.hibernate.validator.internal.metadata.provider.AnnotationMetaDataProvider").getDeclaredMethod("$$ha$resetCache");
                for (Object regAnnotatedDataManager : regAnnotatedMetaDataProviders) {
                    LOGGER.debug("Invoking org.hibernate.validator.internal.metadata.provider.AnnotationMetaDataProvider.$$ha$resetCache on {}", regAnnotatedDataManager);
                    resetCacheMethod1.invoke(regAnnotatedDataManager);
                }
                for (Map.Entry<Object, String> entry : regBeanMetaDataManagersMap.entrySet()) {
                    LOGGER.debug("Invoking " + entry.getValue() + " .$$ha$resetCache on {}", entry.getKey());
                    Method resetCacheMethod2 = resolveClass(entry.getValue()).getDeclaredMethod("$$ha$resetCache");
                    resetCacheMethod2.invoke(entry.getKey());
                }
            } catch (Exception e) {
                LOGGER.error("Error refreshing BeanMetaDataManagerCache/AnnotatedMetaDataProvider cache.", e);
            }
        }
    };


    boolean hibernateEjb;


    public void init(String version, Boolean hibernateEjb) {
        LOGGER.info("Hibernate plugin initialized - Hibernate Core version '{}'", version);
        this.hibernateEjb = hibernateEjb;
    }


    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE)
    public void entityReload(CtClass clazz, Class original) {

        if (AnnotationHelper.hasAnnotation(original, ENTITY_ANNOTATION)
                || AnnotationHelper.hasAnnotation(clazz, ENTITY_ANNOTATION)
                ) {
            LOGGER.debug("Entity reload class {}, original classloader {}", clazz.getName(), original.getClassLoader());
            refresh(100);
        }
    }


    @OnClassFileEvent(classNameRegexp = ".*", events = {FileEvent.CREATE})
    public void newEntity(CtClass clazz) throws Exception {
        if (AnnotationHelper.hasAnnotation(clazz, ENTITY_ANNOTATION)) {
            refresh(500);
        }
    }

    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE)
    public void invalidateClassCache() throws Exception {
        if (!regBeanMetaDataManagersMap.isEmpty() || !regAnnotatedMetaDataProviders.isEmpty()) {
            scheduler.scheduleCommand(invalidateHibernateValidatorCaches);
        }
    }



    private void refresh(int timeout) {
        if (hibernateEjb) {
            scheduler.scheduleCommand(reloadEntityManagerFactoryCommand, timeout);
        } else {
            scheduler.scheduleCommand(reloadSessionFactoryCommand, timeout);
        }
    }

    public void registerAnnotationMetaDataProvider(Object annotatedMetaDataProvider) {
        regAnnotatedMetaDataProviders.add(annotatedMetaDataProvider);
    }


    public void registerBeanMetaDataManager(Object beanMetaDataManager, String beanMetaDataManagerClassName) {
        regBeanMetaDataManagersMap.put(beanMetaDataManager, beanMetaDataManagerClassName);
    }

    private Class<?> resolveClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, appClassLoader);
    }

}

