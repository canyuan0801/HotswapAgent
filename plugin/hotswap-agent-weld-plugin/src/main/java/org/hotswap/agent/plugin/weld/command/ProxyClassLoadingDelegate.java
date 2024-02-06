
package org.hotswap.agent.plugin.weld.command;

import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.ReflectionHelper;
import org.jboss.classfilewriter.ClassFile;
import org.jboss.weld.serialization.spi.ProxyServices;


public class ProxyClassLoadingDelegate {

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

    public static Class<?> loadClass(final ClassLoader classLoader, final String className) throws ClassNotFoundException {
        if (MAGIC_IN_PROGRESS.get()) {
            throw new ClassNotFoundException("HotswapAgent");
        }
        return classLoader.loadClass(className);
    }

    public static Class<?> toClassWeld2(ClassFile ct, ClassLoader loader, ProtectionDomain domain) throws ClassNotFoundException {
        if (MAGIC_IN_PROGRESS.get()) {
            try {
                final Class<?> originalProxyClass = loader.loadClass(ct.getName());
                try {
                    Map<Class<?>, byte[]> reloadMap = new HashMap<>();
                    reloadMap.put(originalProxyClass, ct.toBytecode());

                    PluginManager.getInstance().hotswap(reloadMap);
                    return originalProxyClass;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } catch (ClassNotFoundException e) {
            }
        }
        Class<?> classFileUtilsClass = Class.forName("org.jboss.weld.util.bytecode.ClassFileUtils", true, loader);
        return (Class<?>) ReflectionHelper.invoke(null, classFileUtilsClass, "toClass",
                new Class[] { ClassFile.class, ClassLoader.class, ProtectionDomain.class }, ct, loader, domain);
    }

    public static Class<?> toClassWeld3(Object proxyFactory, ClassFile ct, Class<?> originalClass, ProxyServices proxyServices, ProtectionDomain domain) {
        if (MAGIC_IN_PROGRESS.get()) {
            try {
                ClassLoader loader = originalClass.getClassLoader();
                if (loader == null) {
                    loader = Thread.currentThread().getContextClassLoader();
                }
                final Class<?> originalProxyClass = loader.loadClass(ct.getName());
                try {
                    Map<Class<?>, byte[]> reloadMap = new HashMap<>();
                    reloadMap.put(originalProxyClass, ct.toBytecode());

                    PluginManager.getInstance().hotswap(reloadMap);
                    return originalProxyClass;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } catch (ClassNotFoundException e) {
            }
        }
        return (Class<?>) ReflectionHelper.invoke(proxyFactory, proxyFactory.getClass(), "$$ha$toClass",
                new Class[] { ClassFile.class, Class.class, ProxyServices.class, ProtectionDomain.class }, ct, originalClass, proxyServices, domain);
    }

}
