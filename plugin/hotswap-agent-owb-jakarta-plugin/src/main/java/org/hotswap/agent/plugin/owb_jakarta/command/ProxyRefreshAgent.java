
package org.hotswap.agent.plugin.owb_jakarta.command;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.Interceptor;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.AbstractProducer;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;
import org.apache.webbeans.proxy.NormalScopeProxyFactory;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.owb_jakarta.OwbClassSignatureHelper;
import org.hotswap.agent.util.ReflectionHelper;


public class ProxyRefreshAgent {

    private static AgentLogger LOGGER = AgentLogger.getLogger(ProxyRefreshAgent.class);


    public static synchronized void recreateProxy(ClassLoader appClassLoader, String beanClassName, String oldSignatureForProxyCheck) throws IOException {
        try {
            Class<?> beanClass = appClassLoader.loadClass(beanClassName);
            if (oldSignatureForProxyCheck != null) {
                String newClassSignature = OwbClassSignatureHelper.getSignatureForProxyClass(beanClass);
                if (newClassSignature != null && !newClassSignature.equals(oldSignatureForProxyCheck)) {
                    doRecreateProxy(appClassLoader, beanClass);
                }
            }
        } catch (ClassNotFoundException e) {
            LOGGER.error("Bean class '{}' not found.", beanClassName, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void doRecreateProxy(ClassLoader appClassLoader, Class<?> beanClass) {

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            ProxyClassLoadingDelegate.beginProxyRegeneration();
            Thread.currentThread().setContextClassLoader(appClassLoader);

            WebBeansContext wbc = WebBeansContext.currentInstance();

            AnnotatedElementFactory annotatedElementFactory = wbc.getAnnotatedElementFactory();

            annotatedElementFactory.clear();

            NormalScopeProxyFactory proxyFactory = wbc.getNormalScopeProxyFactory();
            InterceptorDecoratorProxyFactory interceptProxyFactory = wbc.getInterceptorDecoratorProxyFactory();


            Map cachedProxyClasses = (Map) ReflectionHelper.get(proxyFactory, "cachedProxyClasses");
            Map interceptCachedProxyClasses = (Map) ReflectionHelper.get(interceptProxyFactory, "cachedProxyClasses");

            Set<Bean<?>> beans = wbc.getBeanManagerImpl().getBeans(beanClass);
            if (beans != null) {
                Map<Bean, String> proxiedBeans = new LinkedHashMap<>();
                Map<InjectionTargetBean, String> proxiedInterceptBeans = new LinkedHashMap<>();
                for (Bean<?> bean : beans) {
                    if (cachedProxyClasses.containsKey(bean)) {
                        Class proxyClass = (Class) cachedProxyClasses.remove(bean);
                        proxiedBeans.put(bean, proxyClass.getName());
                    }
                    if (interceptCachedProxyClasses.containsKey(bean) && bean instanceof InjectionTargetBean) {
                        Class proxyClass = (Class) interceptCachedProxyClasses.remove(bean);
                        InjectionTargetBean injtBean = (InjectionTargetBean) bean;
                        if (injtBean.getProducer() instanceof AbstractProducer) {
                            AbstractProducer producer = (AbstractProducer) injtBean.getProducer();

                            proxiedInterceptBeans.put(injtBean, proxyClass.getName());
                        }
                    }
                }

                for (Map.Entry<Bean, String> entry : proxiedBeans.entrySet()) {
                    ProxyClassLoadingDelegate.setGeneratingProxyName(entry.getValue());
                    proxyFactory.createProxyClass(entry.getKey(), appClassLoader, beanClass);
                }

                for (Map.Entry<InjectionTargetBean, String> entry : proxiedInterceptBeans.entrySet()) {
                    ProxyClassLoadingDelegate.setGeneratingProxyName(entry.getValue());
                    recreateInterceptedProxy(appClassLoader, entry.getKey(), wbc);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Proxy redefinition failed {}.", e, e.getMessage());
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
            ProxyClassLoadingDelegate.endProxyRegeneration();
            ProxyClassLoadingDelegate.setGeneratingProxyName(null);
        }
    }

    private static void recreateInterceptedProxy(ClassLoader appClassLoader, Bean bean, WebBeansContext wbc) {

        if (!(bean instanceof OwbBean) || bean instanceof Interceptor || bean instanceof Decorator) {
            return;
        }

        OwbBean owbBean = (OwbBean) bean;

        AbstractProducer producer = (AbstractProducer) owbBean.getProducer();
        AnnotatedType annotatedType = ((InjectionTargetBean) owbBean).getAnnotatedType();

        producer.defineInterceptorStack(bean, annotatedType, wbc);
    }
}
