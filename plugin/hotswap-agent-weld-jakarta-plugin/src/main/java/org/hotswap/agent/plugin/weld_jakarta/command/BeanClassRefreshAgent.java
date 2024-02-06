
package org.hotswap.agent.plugin.weld_jakarta.command;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.CDI;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.weld_jakarta.WeldClassSignatureHelper;
import org.hotswap.agent.plugin.weld_jakarta.WeldJakartaPlugin;
import org.hotswap.agent.util.PluginManagerInvoker;
import org.hotswap.agent.util.ReflectionHelper;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.manager.BeanManagerImpl;


public class BeanClassRefreshAgent {

    private static AgentLogger LOGGER = AgentLogger.getLogger(BeanClassRefreshAgent.class);


    public static boolean reloadFlag = false;

    private BeanDeploymentArchive deploymentArchive;

    private String archivePath;

    private boolean registered = false;


    public static void registerArchive(ClassLoader appClassLoader, BeanDeploymentArchive beanArchive, String beanArchiveType) {
        BeansXml beansXml = beanArchive.getBeansXml();

        if (beansXml != null && beansXml.getUrl() != null && (beanArchiveType == null || "EXPLICIT".equals(beanArchiveType) || "IMPLICIT".equals(beanArchiveType))) {
            String archivePath = null;
            String beansXmlPath = beansXml.getUrl().getPath();
            if (beansXmlPath.endsWith("META-INF/beans.xml")) {
                archivePath = beansXmlPath.substring(0, beansXmlPath.length() - "META-INF/beans.xml".length());
            } else if (beansXmlPath.endsWith("WEB-INF/beans.xml")) {
                archivePath = beansXmlPath.substring(0, beansXmlPath.length() - "beans.xml".length()) + "classes";
            }
            if (archivePath.endsWith(".jar!/")) {
                archivePath = archivePath.substring(0, archivePath.length() - "!/".length());
            }

            BeanClassRefreshAgent bdaAgent = null;
            try {
                LOGGER.debug("BeanClassRefreshAgent registerArchive bdaId='{}' archivePath='{}'.", beanArchive.getId(), archivePath);



                @SuppressWarnings("unused")
                File path = new File(archivePath);

                Class<?> registryClass = Class.forName(BdaAgentRegistry.class.getName(), true, appClassLoader);

                boolean contain = (boolean) ReflectionHelper.invoke(null, registryClass, "contains", new Class[] {String.class}, archivePath);

                if (!contain) {
                    bdaAgent = new BeanClassRefreshAgent(beanArchive, archivePath);
                    ReflectionHelper.invoke(null, registryClass, "put", new Class[] {String.class, BeanClassRefreshAgent.class}, archivePath, bdaAgent);
                    bdaAgent.register();
                }
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Unable to watch BeanDeploymentArchive with id={}", beanArchive.getId());
            }
            catch (Exception e) {
                LOGGER.error("Register archive failed.", e.getMessage());
            }
        } else {

        }
    }

    private void register() {
        if (!registered) {
            registered = true;
            PluginManagerInvoker.callPluginMethod(WeldJakartaPlugin.class, getClass().getClassLoader(),
                    "registerBeanDeplArchivePath", new Class[] { String.class }, new Object[] { archivePath });
        }
    }


    public static Collection<BeanClassRefreshAgent> getInstances() {
        return BdaAgentRegistry.values();
    }

    private BeanClassRefreshAgent(BeanDeploymentArchive deploymentArchive, String archivePath) {
        this.deploymentArchive = deploymentArchive;
        this.archivePath = archivePath;
    }


    public String getBdaId() {
        return deploymentArchive.getId();
    }


    public String getArchivePath() {
        return archivePath;
    }


    public BeanDeploymentArchive getDeploymentArchive() {
        return deploymentArchive;
    }


    public static void reloadBean(ClassLoader classLoader, String archivePath, String beanClassName, Map<String, String> oldFullSignatures,
            Map<String, String> oldSignatures, String strReloadStrategy) throws IOException {

        BeanClassRefreshAgent bdaAgent = BdaAgentRegistry.get(archivePath);

        if (bdaAgent == null) {
            LOGGER.error("Archive path '{}' is not associated with any BeanClassRefreshAgent", archivePath);
            return;
        }

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(classLoader);



            Class<?> beanClass = bdaAgent.getClass().getClassLoader().loadClass(beanClassName);

            BeanManagerImpl beanManager;
            if (CDI.current().getBeanManager() instanceof BeanManagerImpl) {
                beanManager = ((BeanManagerImpl) CDI.current().getBeanManager()).unwrap();
            } else {
                beanManager = ((BeanManagerProxy) CDI.current().getBeanManager()).unwrap();
            }

            ClassLoader beanManagerClassLoader = beanManager.getClass().getClassLoader();
            Class<?> bdaAgentClazz = Class.forName(BeanReloadExecutor.class.getName(), true, beanManagerClassLoader);



            ReflectionHelper.invoke(null, bdaAgentClazz, "reloadBean",
                    new Class[] {String.class, Class.class, Map.class, Map.class, String.class },
                    bdaAgent.getBdaId(), beanClass, oldFullSignatures, oldSignatures, strReloadStrategy
            );

        } catch (Exception e) {
            LOGGER.error("Bean reloading failed.", e);
        } finally {
            reloadFlag = false;
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        }
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void recreateProxy(ClassLoader classLoader, String archivePath, Map registeredProxiedBeans, String beanClassName,
            String oldSignatureForProxyCheck) throws IOException {

        BeanClassRefreshAgent bdaAgent = BdaAgentRegistry.get(archivePath);

        if (bdaAgent == null) {
            LOGGER.error("Archive path '{}' is not associated with any BeanClassRefreshAgent", archivePath);
            return;
        }

        try {


            Class<?> beanClass = bdaAgent.getClass().getClassLoader().loadClass(beanClassName);
            bdaAgent.doRecreateProxy(classLoader, registeredProxiedBeans, beanClass, oldSignatureForProxyCheck);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Bean class not found.", e);
        }
    }

    private void doRecreateProxy(ClassLoader classLoader, Map<Object, Object> registeredProxiedBeans, Class<?> beanClass, String oldClassSignature) {
        if (oldClassSignature != null && registeredProxiedBeans != null) {
            String newClassSignature = WeldClassSignatureHelper.getSignatureForProxyClass(beanClass);
            if (newClassSignature != null && !newClassSignature.equals(oldClassSignature)) {
                synchronized (registeredProxiedBeans) {
                    if (!registeredProxiedBeans.isEmpty()) {
                        doRecreateProxy(classLoader, registeredProxiedBeans, beanClass);
                    }
                }
            }
        }
    }

    private void doRecreateProxy(ClassLoader classLoader, Map<Object, Object> registeredBeans, Class<?> proxyClass) {

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            ProxyClassLoadingDelegate.beginProxyRegeneration();
            Class<?> proxyFactoryClass = null;

            for (Entry<Object, Object> entry : registeredBeans.entrySet()) {
                Bean<?> bean = (Bean<?>) entry.getKey();

                if (bean != null) {
                    Set<Type> types = bean.getTypes();
                    if (types.contains(proxyClass)) {
                        Thread.currentThread().setContextClassLoader(bean.getBeanClass().getClassLoader());
                        if (proxyFactoryClass == null) {
                            proxyFactoryClass = classLoader.loadClass("org.jboss.weld.bean.proxy.ProxyFactory");
                        }
                        Object proxyFactory = entry.getValue();
                        LOGGER.info("Recreate proxyClass '{}' for bean class '{}'.", proxyClass.getName(), bean.getClass());
                        ReflectionHelper.invoke(proxyFactory, proxyFactoryClass, "getProxyClass", new Class[] {});
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error("recreateProxyFactory() exception {}.", e, e.getMessage());
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
            ProxyClassLoadingDelegate.endProxyRegeneration();
        }
    }

}
