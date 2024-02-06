
package org.hotswap.agent.plugin.deltaspike.command;

import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.ReflectionHelper;


public class ProxyClassLoadingDelegate {

    private static AgentLogger LOGGER = AgentLogger.getLogger(ProxyClassLoadingDelegate.class);

    private static final ThreadLocal<Boolean> MAGIC_IN_PROGRESS = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    public static final void beginProxyRegeneration() {
        MAGIC_IN_PROGRESS.set(true);
    }

    public static final void endProxyRegeneration() {
        MAGIC_IN_PROGRESS.remove();
    }


    public static Class<?> tryToLoadClassForName(String proxyClassName, Class<?> targetClass, ClassLoader classLoader) {
        if (MAGIC_IN_PROGRESS.get()) {
            return null;
        }
        return (Class<?>) ReflectionHelper.invoke(null, org.apache.deltaspike.core.util.ClassUtils.class, "tryToLoadClassForName",
                new Class[] { String.class, Class.class, ClassLoader.class },
                proxyClassName, targetClass, classLoader);
    }


    public static Class<?> tryToLoadClassForName(String proxyClassName, Class<?> targetClass) {
        if (MAGIC_IN_PROGRESS.get()) {
            return null;
        }
        return org.apache.deltaspike.core.util.ClassUtils.tryToLoadClassForName(proxyClassName, targetClass);
    }

    public static Class<?> loadClass(ClassLoader loader, String className, byte[] bytes, ProtectionDomain protectionDomain) {
        if (MAGIC_IN_PROGRESS.get()) {
            try {
                final Class<?> originalProxyClass = loader.loadClass(className);
                try {
                    Map<Class<?>, byte[]> reloadMap = new HashMap<>();
                    reloadMap.put(originalProxyClass, bytes);
                    PluginManager.getInstance().hotswap(reloadMap);
                    return originalProxyClass;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } catch (ClassNotFoundException e) {

            }
        }
        try {
            Class<?> proxyClassGeneratorClass = null;
            try {

                proxyClassGeneratorClass = loader.loadClass("org.apache.deltaspike.proxy.impl.AsmDeltaSpikeProxyClassGenerator");
            } catch (ClassNotFoundException e1) {
                try {

                    proxyClassGeneratorClass = loader.loadClass("org.apache.deltaspike.proxy.impl.AsmProxyClassGenerator");
                } catch (ClassNotFoundException e2) {
                    LOGGER.error("DeltaspikeProxyClassGenerator class not found!");
                }
            }
            if (proxyClassGeneratorClass != null) {
                return (Class<?>) ReflectionHelper.invoke(null, proxyClassGeneratorClass, "loadClass",
                        new Class[]{ClassLoader.class, String.class, byte[].class, ProtectionDomain.class},
                        loader, className, bytes, protectionDomain);
            }
        } catch (Exception e) {
            LOGGER.error("loadClass() exception {}", e.getMessage());
        }
        return null;
    }


    public static Class<?> defineClass(ClassLoader loader, String className, byte[] bytes, Class<?> originalClass, ProtectionDomain protectionDomain) {
        if (MAGIC_IN_PROGRESS.get()) {
            try {
                final Class<?> originalProxyClass = loader.loadClass(className);
                try {
                    Map<Class<?>, byte[]> reloadMap = new HashMap<>();
                    reloadMap.put(originalProxyClass, bytes);
                    PluginManager.getInstance().hotswap(reloadMap);
                    return originalProxyClass;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } catch (ClassNotFoundException e) {

            }
        }
        try {
            Class<?> classDefiner = null;
            try {

                classDefiner = loader.loadClass("org.apache.deltaspike.proxy.impl.ClassDefiner");
            } catch (ClassNotFoundException e1) {
                LOGGER.error("ClassDefiner class not found!");
            }
            if (classDefiner != null) {
                return (Class<?>) ReflectionHelper.invoke(null, classDefiner, "defineClass",
                        new Class[]{ClassLoader.class, String.class, byte[].class, Class.class, ProtectionDomain.class},
                        loader, className, bytes, originalClass, protectionDomain);
            }
        } catch (Exception e) {
            LOGGER.error("loadClass() exception {}", e.getMessage());
        }
        return null;
    }

}
