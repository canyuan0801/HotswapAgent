
package org.hotswap.agent.plugin.spring.getbean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.spring.SpringPlugin;


public class ProxyReplacer {
    private static AgentLogger LOGGER = AgentLogger.getLogger(ProxyReplacer.class);
    private static Class<?> infrastructureProxyClass;

    public static final String FACTORY_METHOD_NAME = "getBean";


    public static void clearAllProxies() {
        DetachableBeanHolder.detachBeans();
    }


    public static Object register(Object beanFactry, Object bean, Class<?>[] paramClasses, Object[] paramValues) {
        if (bean == null) {
            return bean;
        }
        if (SpringPlugin.basePackagePrefixes != null) {
            boolean hasMatch = false;
            for (String basePackagePrefix : SpringPlugin.basePackagePrefixes) {
                if (bean.getClass().getName().startsWith(basePackagePrefix)) {
                    hasMatch = true;
                    break;
                }
            }


            if (!hasMatch) {
                LOGGER.info("{} not in basePackagePrefix", bean.getClass().getName());
                return bean;
            }
        }


        if (bean.getClass().getName().startsWith("com.sun.proxy.$Proxy")) {
            InvocationHandler handler = new HotswapSpringInvocationHandler(bean, beanFactry, paramClasses, paramValues);
            Class<?>[] interfaces = bean.getClass().getInterfaces();
            try {
                if (!Arrays.asList(interfaces).contains(getInfrastructureProxyClass())) {
                    interfaces = Arrays.copyOf(interfaces, interfaces.length + 1);
                    interfaces[interfaces.length - 1] = getInfrastructureProxyClass();
                }
            } catch (ClassNotFoundException e) {
                LOGGER.error("error adding org.springframework.core.InfrastructureProxy to proxy class", e);
            }



            return Proxy.newProxyInstance(bean.getClass().getClassLoader(), interfaces, handler);
        } else if (EnhancerProxyCreater.isSupportedCglibProxy(bean)) {

            if (bean.getClass().getName().contains("$HOTSWAPAGENT_")) {
                return bean;
            }

            return EnhancerProxyCreater.createProxy(beanFactry, bean, paramClasses, paramValues);
        }

        return bean;
    }

    private static Class<?> getInfrastructureProxyClass() throws ClassNotFoundException {
        if (infrastructureProxyClass == null) {
            infrastructureProxyClass = ProxyReplacer.class.getClassLoader().loadClass("org.springframework.core.InfrastructureProxy");
        }
        return infrastructureProxyClass;
    }
}