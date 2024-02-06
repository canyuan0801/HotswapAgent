
package org.hotswap.agent.plugin.jvm;

import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassMap;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.HotswapTransformer;
import org.hotswap.agent.util.HaClassFileTransformer;
import org.hotswap.agent.util.classloader.ClassLoaderHelper;


@Plugin(name = "AnonymousClassPatch",
        description = "Swap anonymous inner class names to avoid not compatible changes.",
        testedVersions = {"DCEVM"})
public class AnonymousClassPatchPlugin {
    private static AgentLogger LOGGER = AgentLogger.getLogger(AnonymousClassPatchPlugin.class);

    @Init
    static HotswapTransformer hotswapTransformer;



    private static Map<ClassLoader, Map<String, AnonymousClassInfos>> anonymousClassInfosMap =
            new WeakHashMap<ClassLoader, Map<String, AnonymousClassInfos>>();


    @OnClassLoadEvent(classNameRegexp = ".*\\$\\d+", events = LoadEvent.REDEFINE)
    public static CtClass patchAnonymousClass(ClassLoader classLoader, ClassPool classPool, String className, Class original)
            throws IOException, NotFoundException, CannotCompileException {

        String javaClass = className.replaceAll("/", ".");
        String mainClass = javaClass.replaceAll("\\$\\d+$", "");


        if (classPool.find(className) == null)
            return null;

        AnonymousClassInfos info = getStateInfo(classLoader, classPool, mainClass);

        String compatibleName = info.getCompatibleTransition(javaClass);

        if (compatibleName != null) {
            LOGGER.debug("Anonymous class '{}' - replacing with class file {}.", javaClass, compatibleName);
            CtClass ctClass = classPool.get(compatibleName);
            ctClass.replaceClassName(compatibleName, javaClass);
            return ctClass;
        } else {
            LOGGER.debug("Anonymous class '{}' - not compatible change is replaced with empty implementation.", javaClass, compatibleName);


            CtClass ctClass = classPool.makeClass(javaClass);

            ctClass.setSuperclass(classPool.get(original.getSuperclass().getName()));

            Class[] originalInterfaces = original.getInterfaces();
            CtClass[] interfaces = new CtClass[originalInterfaces.length];
            for (int i = 0; i < originalInterfaces.length; i++)
                interfaces[i] = classPool.get(originalInterfaces[i].getName());
            ctClass.setInterfaces(interfaces);

            return ctClass;





        }
    }

    private static boolean isHotswapAgentSyntheticClass(String compatibleName) {
        String anonymousClassIndexString = compatibleName.replaceAll("^.*\\$(\\d+)$", "$1");
        try {
            long anonymousClassIndex = Long.valueOf(anonymousClassIndexString);
            return anonymousClassIndex >= AnonymousClassInfos.UNIQUE_CLASS_START_INDEX;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(compatibleName + " is not in a format of className$i");
        }
    }



    private static void registerReplaceOnLoad(final String newName, final CtClass anonymous) {
        hotswapTransformer.registerTransformer(null, newName, new HaClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                LOGGER.trace("Anonymous class '{}' - replaced.", newName);
                hotswapTransformer.removeTransformer(newName, this);
                try {
                    return anonymous.toBytecode();
                } catch (Exception e) {
                    LOGGER.error("Unable to create bytecode of class {}.", e, anonymous.getName());
                    return null;
                }
            }
            @Override
            public boolean isForRedefinitionOnly() {
                return false;
            }
        });
    }


    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE)
    public static byte[] patchMainClass(String className, ClassPool classPool, CtClass ctClass,
                                        ClassLoader classLoader, ProtectionDomain protectionDomain) throws IOException, CannotCompileException, NotFoundException {
        String javaClassName = className.replaceAll("/", ".");


        if (!ClassLoaderHelper.isClassLoaded(classLoader, javaClassName + "$1"))
            return null;


        AnonymousClassInfos stateInfo = getStateInfo(classLoader, classPool, javaClassName);
        Map<AnonymousClassInfo, AnonymousClassInfo> transitions = stateInfo.getCompatibleTransitions();

        ClassMap replaceClassNameMap = new ClassMap();
        for (Map.Entry<AnonymousClassInfo, AnonymousClassInfo> entry : transitions.entrySet()) {
            String compatibleName = entry.getKey().getClassName();
            String newName = entry.getValue().getClassName();

            if (!newName.equals(compatibleName)) {
                replaceClassNameMap.put(newName, compatibleName);
                LOGGER.trace("Class '{}' replacing '{}' for '{}'", javaClassName, newName, compatibleName);
            }


            if (isHotswapAgentSyntheticClass(compatibleName)) {
                LOGGER.debug("Anonymous class '{}' not comatible and is replaced with synthetic class '{}'", newName, compatibleName);

                CtClass anonymous = classPool.get(newName);
                anonymous.replaceClassName(newName, compatibleName);
                anonymous.toClass(classLoader, protectionDomain);
            } else if (!ClassLoaderHelper.isClassLoaded(classLoader, newName)) {
                CtClass anonymous = classPool.get(compatibleName);
                anonymous.replaceClassName(compatibleName, newName);


                LOGGER.debug("Anonymous class '{}' - will be replaced from class file {}.", newName, compatibleName);
                registerReplaceOnLoad(newName, anonymous);
            }
        }





        ctClass.replaceClassName(replaceClassNameMap);

        LOGGER.reload("Class '{}' has been enhanced with anonymous classes for hotswap.", className);
        return ctClass.toBytecode();
    }


    private static synchronized AnonymousClassInfos getStateInfo(ClassLoader classLoader, ClassPool classPool, String className) {
        Map<String, AnonymousClassInfos> classInfosMap = getClassInfosMapForClassLoader(classLoader);

        AnonymousClassInfos infos = classInfosMap.get(className);

        if (infos == null || !infos.isCurrent(classPool)) {
            if (infos == null)
                LOGGER.trace("Creating new infos for className {}", className);
            else
                LOGGER.trace("Creating new infos, current is obsolete for className {}", className);

            infos = new AnonymousClassInfos(classPool, className);
            infos.mapPreviousState(new AnonymousClassInfos(classLoader, className));
            classInfosMap.put(className, infos);
        } else {
            LOGGER.trace("Returning existing infos for className {}", className);
        }
        return infos;
    }


    private static Map<String, AnonymousClassInfos> getClassInfosMapForClassLoader(ClassLoader classLoader) {
        Map<String, AnonymousClassInfos> classInfosMap = anonymousClassInfosMap.get(classLoader);
        if (classInfosMap == null) {
            synchronized (classLoader) {
                classInfosMap = anonymousClassInfosMap.get(classLoader);
                if (classInfosMap == null) {
                    classInfosMap = new HashMap<>();
                    anonymousClassInfosMap.put(classLoader, classInfosMap);
                }
            }
        }
        return classInfosMap;
    }
}
