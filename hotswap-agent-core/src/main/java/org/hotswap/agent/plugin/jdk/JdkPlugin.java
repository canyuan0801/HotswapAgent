
package org.hotswap.agent.plugin.jdk;

import java.util.Map;

import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.bytecode.ClassFile;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.ReflectionHelper;


@Plugin(name = "JdkPlugin",
        description = "",
        testedVersions = {"openjdk 1.7.0.95, 1.8.0_74, 1.11.0_5"},
        expectedVersions = {"All between openjdk 1.7 - 1.11"}
        )
public class JdkPlugin {

    private static AgentLogger LOGGER = AgentLogger.getLogger(JdkPlugin.class);


    public static boolean reloadFlag;

    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE, skipSynthetic=false)
    public static void flushBeanIntrospectorCaches(ClassLoader classLoader, CtClass ctClass) {
        try {
            LOGGER.debug("Flushing {} from introspector", ctClass.getName());

            Class<?> clazz = classLoader.loadClass(ctClass.getName());
            Class<?> threadGroupCtxClass = classLoader.loadClass("java.beans.ThreadGroupContext");
            Class<?> introspectorClass = classLoader.loadClass("java.beans.Introspector");

            synchronized (classLoader) {
                Object contexts = ReflectionHelper.get(null, threadGroupCtxClass, "contexts");
                Object table[] = (Object[]) ReflectionHelper.get(contexts, "table");

                if (table != null) {
                    for (Object o: table) {
                        if (o != null) {
                            Object threadGroupContext = ReflectionHelper.get(o, "value");
                            if (threadGroupContext != null) {
                                LOGGER.trace("Removing from threadGroupContext");
                                ReflectionHelper.invoke(threadGroupContext, threadGroupCtxClass, "removeBeanInfo",
                                        new Class[] { Class.class }, clazz);
                            }
                        }
                    }
                }

                ReflectionHelper.invoke(null, introspectorClass, "flushFromCaches",
                    new Class[] { Class.class }, clazz);
            }
        } catch (Exception e) {
            LOGGER.error("flushBeanIntrospectorCaches() exception {}.", e.getMessage());
        } finally {
            reloadFlag = false;
        }
    }

    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE, skipSynthetic=false)
    public static void flushObjectStreamCaches(ClassLoader classLoader, CtClass ctClass) {
        Class<?> clazz;
        Object localDescs;
        Object reflectors;
        try {
            LOGGER.debug("Flushing {} from ObjectStreamClass caches", ctClass.getName());

            clazz = classLoader.loadClass(ctClass.getName());
            Class<?> objectStreamClassCache = classLoader.loadClass("java.io.ObjectStreamClass$Caches");

            localDescs = ReflectionHelper.get(null, objectStreamClassCache, "localDescs");
            reflectors = ReflectionHelper.get(null, objectStreamClassCache, "reflectors");

        } catch (Exception e) {
            LOGGER.error("flushObjectStreamCaches() java.io.ObjectStreamClass$Caches not found.", e.getMessage());
            return;
        }

        boolean java17;

        try {
            ((Map) localDescs).clear();
            ((Map) reflectors).clear();
            java17 = false;
        } catch (Exception e) {
            java17 = true;
        }

        if (java17) {
            try {
                Object localDescsMap = ReflectionHelper.get(localDescs, "map");
                ReflectionHelper.invoke(localDescsMap, localDescsMap.getClass(), "remove", new Class[] { Class.class }, clazz);
                Object reflectorsMap = ReflectionHelper.get(reflectors, "map");
                ReflectionHelper.invoke(reflectorsMap, reflectorsMap.getClass(), "remove", new Class[] { Class.class }, clazz);
            } catch (Exception e) {
                LOGGER.error("flushObjectStreamCaches() exception {}.", e.getMessage());
            }
        }
    }

}
