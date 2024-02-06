
package org.hotswap.agent.plugin.mybatis.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.session.Configuration;
import org.hotswap.agent.javassist.util.proxy.MethodHandler;
import org.hotswap.agent.javassist.util.proxy.ProxyFactory;
import org.hotswap.agent.plugin.mybatis.transformers.MyBatisTransformers;
import org.hotswap.agent.util.ReflectionHelper;


public class ConfigurationProxy {
    private static Map<XMLConfigBuilder, ConfigurationProxy> proxiedConfigurations = new HashMap<>();

    public static ConfigurationProxy getWrapper(XMLConfigBuilder configBuilder) {
        if (!proxiedConfigurations.containsKey(configBuilder)) {
            proxiedConfigurations.put(configBuilder, new ConfigurationProxy(configBuilder));
        }
        return proxiedConfigurations.get(configBuilder);
    }

    public static void refreshProxiedConfigurations() {
        for (ConfigurationProxy wrapper : proxiedConfigurations.values())
            try {
                wrapper.refreshProxiedConfiguration();
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    private ConfigurationProxy(XMLConfigBuilder configBuilder) {
        this.configBuilder = configBuilder;
    }

    public void refreshProxiedConfiguration() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this.configuration = new Configuration();
        ReflectionHelper.invoke(configBuilder, MyBatisTransformers.REFRESH_METHOD);
    }

    private XMLConfigBuilder configBuilder;
    private Configuration configuration;
    private Configuration proxyInstance;

    public Configuration proxy(Configuration origConfiguration) {
        this.configuration = origConfiguration;
        if (proxyInstance == null) {
            ProxyFactory factory = new ProxyFactory();
            factory.setSuperclass(Configuration.class);

            MethodHandler handler = new MethodHandler() {
                @Override
                public Object invoke(Object self, Method overridden, Method forwarder,
                                     Object[] args) throws Throwable {
                    return overridden.invoke(configuration, args);
                }
            };

            try {
                proxyInstance = (Configuration) factory.create(new Class[0], null, handler);
            } catch (Exception e) {
                throw new Error("Unable instantiate Configuration proxy", e);
            }
        }
        return proxyInstance;
    }
}