
package org.hotswap.agent.plugin.owb_jakarta.command;

import java.util.HashMap;
import java.util.Map;

import org.apache.webbeans.proxy.AbstractProxyFactory;
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

    private static String generatingProxyName;

    public static void setGeneratingProxyName(String generatingProxyName) {
        ProxyClassLoadingDelegate.generatingProxyName = generatingProxyName;
    }

    public static final void beginProxyRegeneration() {
        MAGIC_IN_PROGRESS.set(true);
    }

    public static final void endProxyRegeneration() {
        MAGIC_IN_PROGRESS.remove();
    }

    public static Class<?> forName(String name, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
        if (MAGIC_IN_PROGRESS.get()) {
            if (generatingProxyName == null || generatingProxyName.equals(name)) {
                throw new ClassNotFoundException("HotswapAgent");
            }
        }
        return Class.forName(name, initialize, loader);
    }

    public static Class<?> defineAndLoadClass(AbstractProxyFactory proxyFactory, ClassLoader classLoader, String proxyName, byte[] proxyBytes) {
        if (MAGIC_IN_PROGRESS.get()) {
            Class<?> reloaded = reloadProxyByteCode(classLoader, proxyName, proxyBytes, null);
            if (reloaded != null) {
                return reloaded;
            }
        }
        try {
            return (Class<?>) ReflectionHelper.invoke(proxyFactory, AbstractProxyFactory.class, "defineAndLoadClass",
                    new Class[]{ClassLoader.class, String.class, byte[].class},
                    classLoader, proxyName, proxyBytes);
        } catch (Exception e) {
            LOGGER.error("defineAndLoadClass() exception {}", e.getMessage());
        }
        return null;
    }

    public static Class<?> defineAndLoadClassWithUnsafe(Object unsafe, ClassLoader classLoader, String proxyName, byte[] proxyBytes) {
        if (MAGIC_IN_PROGRESS.get()) {
            Class<?> reloaded = reloadProxyByteCode(classLoader, proxyName, proxyBytes, null);
            if (reloaded != null) {
                return reloaded;
            }
        }
        try {
            return (Class<?>) ReflectionHelper.invoke(unsafe, unsafe.getClass(), "defineAndLoadClass",
                    new Class[]{ClassLoader.class, String.class, byte[].class},
                    classLoader, proxyName, proxyBytes);
        } catch (Exception e) {
            LOGGER.error("defineAndLoadClass() exception {}", e.getMessage());
        }
        return null;
    }

    public static Class<?> defineAndLoadClassWithUnsafe(Object unsafe, ClassLoader classLoader, String proxyName, byte[] proxyBytes, Class<?> classToProxy) {
        if (MAGIC_IN_PROGRESS.get()) {
            Class<?> reloaded = reloadProxyByteCode(classLoader, proxyName, proxyBytes, classToProxy);
            if (reloaded != null) {
                return reloaded;
            }
        }
        try {
            return (Class<?>) ReflectionHelper.invoke(unsafe, unsafe.getClass(), "defineAndLoadClass",
                    new Class[]{ClassLoader.class, String.class, byte[].class, Class.class},
                    classLoader, proxyName, proxyBytes, classToProxy);
        } catch (Exception e) {
            LOGGER.error("defineAndLoadClass() exception {}", e.getMessage());
        }
        return null;
    }

    private static Class<?> reloadProxyByteCode(ClassLoader classLoader, String proxyName, byte[] proxyBytes, Class<?> classToProxy) {
        try {
            final Class<?> originalProxyClass = classLoader.loadClass(proxyName);
            try {
                Map<Class<?>, byte[]> reloadMap = new HashMap<>();
                reloadMap.put(originalProxyClass, proxyBytes);

                PluginManager.getInstance().hotswap(reloadMap);
                return originalProxyClass;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (ClassNotFoundException e) {

        }
        return null;
    }
}
