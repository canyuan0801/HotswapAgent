
package org.hotswap.agent.util.classloader;

import java.lang.reflect.Method;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.ReflectionHelper;


public class ClassLoaderHelper {
    private static AgentLogger LOGGER = AgentLogger.getLogger(ClassLoaderHelper.class);

    public static Method findLoadedClass;

    static {
        try {
            findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
            findLoadedClass.setAccessible(true);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Unexpected: failed to get ClassLoader findLoadedClass method", e);
        }
    }



    public static boolean isClassLoaded(ClassLoader classLoader, String className) {
        try {
            return findLoadedClass.invoke(classLoader, className) != null;
        } catch (Exception e) {
            LOGGER.error("Unable to invoke findLoadedClass on classLoader {}, className {}", e, classLoader, className);
            return false;
        }
    }


    public static boolean isClassLoderStarted(ClassLoader classLoader) {

        String classLoaderClassName = (classLoader != null) ? classLoader.getClass().getName() : null;


        if ("org.glassfish.web.loader.WebappClassLoader".equals(classLoaderClassName)||
            "org.apache.catalina.loader.WebappClassLoader".equals(classLoaderClassName) ||
            "org.apache.catalina.loader.ParallelWebappClassLoader".equals(classLoaderClassName) ||
            "org.apache.tomee.catalina.TomEEWebappClassLoader".equals(classLoaderClassName) ||
            "org.springframework.boot.web.embedded.tomcat.TomcatEmbeddedWebappClassLoader".equals(classLoaderClassName)
            )
        {
            try {
                Class<?> clazz = classLoader.getClass();
                boolean isStarted;
                if ("org.apache.catalina.loader.WebappClassLoaderBase".equals(clazz.getSuperclass().getName())) {
                    clazz = clazz.getSuperclass();
                    isStarted = "STARTED".equals((String) ReflectionHelper.invoke(classLoader, clazz, "getStateName", new Class[] {}, null));
                } else {
                    isStarted = (boolean) ReflectionHelper.invoke(classLoader, clazz, "isStarted", new Class[] {}, null);
                }
                return isStarted;
            } catch (Exception e) {
                LOGGER.warning("isClassLoderStarted() : {}", e.getMessage());
            }
        }
        return true;
    }
}
