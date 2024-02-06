
package org.hotswap.agent.plugin.hibernate;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.CtNewMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.AccessFlag;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.hibernate.proxy.SessionFactoryProxy;
import org.hotswap.agent.util.PluginManagerInvoker;


public class HibernateTransformers {
    private static AgentLogger LOGGER = AgentLogger.getLogger(HibernateTransformers.class);

    private static Boolean isJavax;

    private static boolean isJavax(ClassPool classPool) {
        if (isJavax == null) {
            try {
                classPool.get("javax.persistence.EntityManager");
                isJavax = true;
            } catch (NotFoundException e) {
                isJavax = false;
            }
        }
        return isJavax;
    }


    @OnClassLoadEvent(classNameRegexp = "(org.hibernate.ejb.HibernatePersistence)|(org.hibernate.jpa.HibernatePersistenceProvider)|(org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider)|(org.springframework.orm.jpa.vendor.SpringHibernateEjbPersistenceProvider)")
    public static void proxyHibernatePersistence(ClassPool classPool, CtClass clazz) throws Exception {
        if (!isJavax(classPool)) {
            return;
        }

        LOGGER.debug("Override org.hibernate.ejb.HibernatePersistence#createContainerEntityManagerFactory and createEntityManagerFactory to create a EntityManagerFactoryProxy proxy.");

        CtMethod oldMethod = clazz.getDeclaredMethod("createContainerEntityManagerFactory");
        oldMethod.setName("$$ha$createContainerEntityManagerFactory" + clazz.getSimpleName());
        CtMethod newMethod = CtNewMethod.make(
                "public javax.persistence.EntityManagerFactory createContainerEntityManagerFactory(" +
                        "           javax.persistence.spi.PersistenceUnitInfo info, java.util.Map properties) {" +
                        "  properties.put(\"PERSISTENCE_CLASS_NAME\", \"" + clazz.getName() + "\");" +
                        "  return " + HibernatePersistenceHelper.class.getName() + ".createContainerEntityManagerFactoryProxy(" +
                        "      this, info, properties, $$ha$createContainerEntityManagerFactory" + clazz.getSimpleName() + "(info, properties)); " +
                        "}", clazz);
        clazz.addMethod(newMethod);

        try {
            oldMethod = clazz.getDeclaredMethod("createEntityManagerFactory");
            oldMethod.setName("$$ha$createEntityManagerFactory" + clazz.getSimpleName());

            newMethod = CtNewMethod.make(
                    "public javax.persistence.EntityManagerFactory createEntityManagerFactory(" +
                            "           String persistenceUnitName, java.util.Map properties) {" +
                            "  return " + HibernatePersistenceHelper.class.getName() + ".createEntityManagerFactoryProxy(" +
                            "      this, persistenceUnitName, properties, $$ha$createEntityManagerFactory" + clazz.getSimpleName() + "(persistenceUnitName, properties)); " +
                            "}", clazz);
            clazz.addMethod(newMethod);
        } catch (NotFoundException e) {
            LOGGER.trace("Method createEntityManagerFactory not found on " + clazz.getName() + ". Is Ok for Spring implementation...", e);
        }
    }


    @OnClassLoadEvent(classNameRegexp = "org.hibernate.internal.SessionFactoryImpl")
    public static void removeSessionFactoryImplFinalFlag(ClassPool classPool, CtClass clazz) throws Exception {
        if (!isJavax(classPool)) {
            return;
        }
        clazz.getClassFile().setAccessFlags(AccessFlag.PUBLIC);
    }

    @OnClassLoadEvent(classNameRegexp = "org.hibernate.cfg.Configuration")
    public static void proxySessionFactory(ClassLoader classLoader, ClassPool classPool, CtClass clazz) throws Exception {
        if (!isJavax(classPool)) {
            return;
        }


        if (checkHibernateEjb(classLoader))
            return;

        LOGGER.debug("Override org.hibernate.cfg.Configuration#buildSessionFactory to create a SessionFactoryProxy proxy.");

        CtClass serviceRegistryClass = classPool.makeClass("org.hibernate.service.ServiceRegistry");
        CtMethod oldMethod = clazz.getDeclaredMethod("buildSessionFactory", new CtClass[]{serviceRegistryClass});
        oldMethod.setName("$$ha$buildSessionFactory");

        CtMethod newMethod = CtNewMethod.make(
                "public org.hibernate.SessionFactory buildSessionFactory(org.hibernate.service.ServiceRegistry serviceRegistry) throws org.hibernate.HibernateException {" +
                        "  return " + SessionFactoryProxy.class.getName() + ".getWrapper(this)" +
                        "       .proxy($$ha$buildSessionFactory(serviceRegistry), serviceRegistry); " +
                        "}", clazz);
        clazz.addMethod(newMethod);
    }


    private static boolean checkHibernateEjb(ClassLoader classLoader) {
        try {
            classLoader.loadClass("org.hibernate.ejb.HibernatePersistence");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @OnClassLoadEvent(classNameRegexp = "(org.hibernate.validator.internal.metadata.BeanMetaDataManager)|(org.hibernate.validator.internal.metadata.BeanMetaDataManagerImpl)")
    public static void beanMetaDataManagerRegisterVariable(ClassPool classPool, CtClass ctClass) throws CannotCompileException {
        if (!isJavax(classPool)) {
            return;
        }
        StringBuilder src = new StringBuilder("{");
        src.append(PluginManagerInvoker.buildInitializePlugin(HibernatePlugin.class));
        src.append(PluginManagerInvoker.buildCallPluginMethod(HibernatePlugin.class, "registerBeanMetaDataManager",
                "this", "java.lang.Object", "this.getClass().getName()", "java.lang.String"));
        src.append("}");
        for (CtConstructor constructor : ctClass.getDeclaredConstructors()) {
            constructor.insertAfter(src.toString());
        }
        try {
          ctClass.addMethod(CtNewMethod.make("public void $$ha$resetCache() {" +
                  "   this.beanMetaDataCache.clear(); " +
                  "}", ctClass));
        } catch (org.hotswap.agent.javassist.CannotCompileException e) {
            LOGGER.trace("Field beanMetaDataCache not found on " + ctClass.getName() + ". Is Ok for BeanMetaDataManager interface.", e);
        }

        LOGGER.debug("org.hibernate.validator.internal.metadata.BeanMetaDataManager - added method $$ha$resetCache().");
    }

    @OnClassLoadEvent(classNameRegexp = "org.hibernate.validator.internal.metadata.provider.AnnotationMetaDataProvider")
    public static void annotationMetaDataProviderRegisterVariable(ClassPool classPool, CtClass ctClass) throws CannotCompileException {
        if (!isJavax(classPool)) {
            return;
        }
        StringBuilder src = new StringBuilder("{");
        src.append(PluginManagerInvoker.buildInitializePlugin(HibernatePlugin.class));
        src.append(PluginManagerInvoker.buildCallPluginMethod(HibernatePlugin.class, "registerAnnotationMetaDataProvider",
                "this", "java.lang.Object"));
        src.append("}");
        for (CtConstructor constructor : ctClass.getDeclaredConstructors()) {
            constructor.insertAfter(src.toString());
        }
        try {
            ctClass.getDeclaredField("configuredBeans");
            ctClass.addMethod(CtNewMethod.make(
                    "public void $$ha$resetCache() {"
                  + "   this.configuredBeans.clear(); " + "}",
                    ctClass));
        } catch (org.hotswap.agent.javassist.NotFoundException e) {

            ctClass.addMethod(CtNewMethod.make(
                    "public void $$ha$resetCache() {"
                  + "}",
                    ctClass));
        }
        LOGGER.debug("org.hibernate.validator.internal.metadata.provider.AnnotationMetaDataProvider - added method $$ha$resetCache().");
    }

}
