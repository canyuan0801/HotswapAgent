
package org.hotswap.agent.plugin.spring.core;

import java.lang.reflect.Field;
import java.util.Map;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.ReflectionHelper;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;


public class ResetSpringStaticCaches {
    private static AgentLogger LOGGER = AgentLogger.getLogger(ResetSpringStaticCaches.class);


    public static void resetBeanNamesByType(DefaultListableBeanFactory defaultListableBeanFactory) {
        try {
            Field field = DefaultListableBeanFactory.class.getDeclaredField("singletonBeanNamesByType");
            field.setAccessible(true);

            Map singletonBeanNamesByType = (Map) field.get(defaultListableBeanFactory);
            singletonBeanNamesByType.clear();
        } catch (Exception e) {
            LOGGER.trace("Unable to clear DefaultListableBeanFactory.singletonBeanNamesByType cache (is Ok for pre 3.1.2 Spring version)", e);
        }

        try {
            Field field = DefaultListableBeanFactory.class.getDeclaredField("allBeanNamesByType");
            field.setAccessible(true);

            Map allBeanNamesByType = (Map) field.get(defaultListableBeanFactory);
            allBeanNamesByType.clear();
        } catch (Exception e) {
            LOGGER.trace("Unable to clear allBeanNamesByType cache (is Ok for pre 3.2 Spring version)");
        }

        try {
            Field field = DefaultListableBeanFactory.class.getDeclaredField("nonSingletonBeanNamesByType");
            field.setAccessible(true);

            Map nonSingletonBeanNamesByType = (Map) field.get(defaultListableBeanFactory);
            nonSingletonBeanNamesByType.clear();
        } catch (Exception e) {
            LOGGER.debug("Unable to clear nonSingletonBeanNamesByType cache (is Ok for pre 3.2 Spring version)");
        }

    }


    public static void reset() {
        resetTypeVariableCache();
        resetAnnotationUtilsCache();
        resetReflectionUtilsCache();
        resetResolvableTypeCache();
        resetPropertyCache();
        CachedIntrospectionResults.clearClassLoader(ResetSpringStaticCaches.class.getClassLoader());
    }

    private static void resetResolvableTypeCache() {
        ReflectionHelper.invokeNoException(null, "org.springframework.core.ResolvableType",
                ResetSpringStaticCaches.class.getClassLoader(), "clearCache", new Class<?>[] {});
    }

    private static void resetTypeVariableCache() {
        try {
            Field field = GenericTypeResolver.class.getDeclaredField("typeVariableCache");
            field.setAccessible(true);

            Map<Class, Map> typeVariableCache = (Map<Class, Map>) field.get(null);
            typeVariableCache.clear();
            LOGGER.trace("Cache cleared: GenericTypeResolver.typeVariableCache");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to clear GenericTypeResolver.typeVariableCache", e);
        }
    }

    private static void resetReflectionUtilsCache() {
        ReflectionHelper.invokeNoException(null, "org.springframework.util.ReflectionUtils",
                ResetSpringStaticCaches.class.getClassLoader(), "clearCache", new Class<?>[] {});

        Map declaredMethodsCache = (Map) ReflectionHelper.getNoException(null, ReflectionUtils.class,
                "declaredMethodsCache");
        if (declaredMethodsCache != null) {
            declaredMethodsCache.clear();
            LOGGER.trace("Cache cleared: ReflectionUtils.declaredMethodsCache");
        } else {
            LOGGER.trace("Cache NOT cleared: ReflectionUtils.declaredMethodsCache not exists");
        }
    }

    private static void resetAnnotationUtilsCache() {
        ReflectionHelper.invokeNoException(null, "org.springframework.core.annotation.AnnotationUtils",
                ResetSpringStaticCaches.class.getClassLoader(), "clearCache", new Class<?>[] {});

        Map annotatedInterfaceCache = (Map) ReflectionHelper.getNoException(null, AnnotationUtils.class,
                "annotatedInterfaceCache");
        if (annotatedInterfaceCache != null) {
            annotatedInterfaceCache.clear();
            LOGGER.trace("Cache cleared: AnnotationUtils.annotatedInterfaceCache");
        } else {
            LOGGER.trace("Cache NOT cleared: AnnotationUtils.annotatedInterfaceCache not exists in target Spring verion (pre 3.1.x)");
        }

        Map findAnnotationCache = (Map) ReflectionHelper.getNoException(null, AnnotationUtils.class, "findAnnotationCache");
        if (findAnnotationCache != null) {
            findAnnotationCache.clear();
            LOGGER.trace("Cache cleared: AnnotationUtils.findAnnotationCache");
        } else {
            LOGGER.trace("Cache NOT cleared: AnnotationUtils.findAnnotationCache not exists in target Spring version (pre 4.1)");
        }

    }

    private static void resetPropertyCache() {
        try {
            ClassLoader classLoader = ResetSpringStaticCaches.class.getClassLoader();
            Map annotationCache = (Map) ReflectionHelper.get(null,
                    classLoader.loadClass("org.springframework.core.convert.Property"), "annotationCache");
            annotationCache.clear();
            LOGGER.trace("Cache cleared: Property.annotationCache");
        } catch (Exception e) {
            LOGGER.trace("Unable to clear Property.annotationCache (ok before Spring 3.2.x)", e);
        }
    }
}
