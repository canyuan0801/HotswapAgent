
package org.hotswap.agent.plugin.hibernate3.jpa;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import org.hotswap.agent.annotation.FileEvent;
import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.Maven;
import org.hotswap.agent.annotation.OnClassFileEvent;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.OnResourceFileEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.annotation.Versions;
import org.hotswap.agent.command.Command;
import org.hotswap.agent.command.ReflectionCommand;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.hibernate3.jpa.Hibernate3JPARefreshCommands;
import org.hotswap.agent.util.AnnotationHelper;



@Plugin(name = "Hibernate3JPA",
        group = "groupHibernate",
        description = "Reload Hibernate configuration after entity create/change.",
        testedVersions = { "3.6" },
        expectedVersions = { "3.6" },
        supportClass = { Hibernate3JPATransformers.class })
@Versions(maven = { @Maven(value = "[3.0,4.0)", artifactId = "hibernate-entitymanager", groupId = "org.hibernate") })
public class Hibernate3JPAPlugin {


    private static final String ENTITY_ANNOTATION = "javax.persistence.Entity";


    private static AgentLogger LOGGER = AgentLogger.getLogger(Hibernate3JPAPlugin.class);


    @Init
    Scheduler scheduler;


    @Init
    ClassLoader appClassLoader;


    Set<Object> regAnnotatedMetaDataProviders = Collections.newSetFromMap(new WeakHashMap<Object, Boolean>());


    Set<Object> regBeanMetaDataManagers = Collections.newSetFromMap(new WeakHashMap<Object, Boolean>());



    Command reloadEntityManagerFactoryCommand = new ReflectionCommand(this, Hibernate3JPARefreshCommands.class.getName(),
            "reloadEntityManagerFactory");



    private Command invalidateHibernateValidatorCaches = new Command() {
        @Override
        public void executeCommand() {
            LOGGER.debug("Refreshing BeanMetaDataManagerCache/AnnotatedMetaDataProvider cache.");

            try {
                Method resetCacheMethod1 = resolveClass(
                        "org.hibernate.validator.internal.metadata.provider.AnnotationMetaDataProvider")
                                .getDeclaredMethod("__resetCache");
                for (Object regAnnotatedDataManager : regAnnotatedMetaDataProviders) {
                    LOGGER.debug(
                            "Invoking org.hibernate.validator.internal.metadata.provider.AnnotationMetaDataProvider.__resetCache on {}",
                            regAnnotatedDataManager);
                    resetCacheMethod1.invoke(regAnnotatedDataManager);
                }
                Method resetCacheMethod2 = resolveClass("org.hibernate.validator.internal.metadata.BeanMetaDataManager")
                        .getDeclaredMethod("__resetCache");
                for (Object regBeanMetaDataManager : regBeanMetaDataManagers) {
                    LOGGER.debug(
                            "Invoking org.hibernate.validator.internal.metadata.BeanMetaDataManager.__resetCache on {}",
                            regBeanMetaDataManager);
                    resetCacheMethod2.invoke(regBeanMetaDataManager);
                }
            } catch (Exception e) {
                LOGGER.error("Error refreshing BeanMetaDataManagerCache/AnnotatedMetaDataProvider cache.", e);
            }
        }
    };



    boolean hibernateEjb;


    public void init(String version, Boolean hibernateEjb) {
        LOGGER.info("Hibernate3 JPA plugin initialized - Hibernate Core version '{}'", version);
        this.hibernateEjb = hibernateEjb;
    }


    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE)
    public void entityReload(CtClass clazz, Class<?> original) {


        if (AnnotationHelper.hasAnnotation(original, ENTITY_ANNOTATION)
                || AnnotationHelper.hasAnnotation(clazz, ENTITY_ANNOTATION)) {
            LOGGER.debug("Entity reload class {}, original classloader {}", clazz.getName(), original.getClassLoader());
            refresh(100);
        }
    }


    @OnClassFileEvent(classNameRegexp = ".*", events = { FileEvent.CREATE })
    public void newEntity(CtClass clazz) throws Exception {
        if (AnnotationHelper.hasAnnotation(clazz, ENTITY_ANNOTATION)) {
            refresh(500);
        }
    }


    @OnResourceFileEvent(path = "/", filter = ".*.hbm.xml")
    public void refreshOnHbm(){
        refresh(500);
    }


    @OnResourceFileEvent(path = "/", filter = ".*.cfg.xml")
    public void refreshOnCfg(){
        refresh(500);
    }



    @OnResourceFileEvent(path = "/", filter = "persistence.xml")
    public void refreshOnPersistenceXml(){
        refresh(500);
    }



    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE)
    public void invalidateClassCache() throws Exception {
        if (!regBeanMetaDataManagers.isEmpty() || !regAnnotatedMetaDataProviders.isEmpty()) {
            scheduler.scheduleCommand(invalidateHibernateValidatorCaches);
        }
    }





    private void refresh(int timeout) {
        scheduler.scheduleCommand(reloadEntityManagerFactoryCommand, timeout);
    }


    public void registerAnnotationMetaDataProvider(Object annotatedMetaDataProvider) {
        regAnnotatedMetaDataProviders.add(annotatedMetaDataProvider);
    }


    public void registerBeanMetaDataManager(Object beanMetaDataManager) {
        regBeanMetaDataManagers.add(beanMetaDataManager);
    }


    private Class<?> resolveClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, appClassLoader);
    }

}
