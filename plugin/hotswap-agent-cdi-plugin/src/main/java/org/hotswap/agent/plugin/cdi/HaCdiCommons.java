
package org.hotswap.agent.plugin.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtField;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.ReflectionHelper;


public class HaCdiCommons {

    private static final AgentLogger LOGGER = AgentLogger.getLogger(HaCdiCommons.class);

    private static final String BEAN_REGISTRY_FIELD = "$$ha$beanRegistry";
    private static final Map<Class<? extends Annotation>, Class<?>> scopeToContextMap = new HashMap<>();
    private static final Map<HaCdiExtraContext, Boolean> extraContexts = new HashMap<>();

    private static Boolean isJakarta;

    public static boolean isJakarta(ClassPool classPool) {
        if (isJakarta == null) {
            try {
                classPool.get("jakarta.enterprise.context.spi.Contextual");
                isJakarta = true;
            } catch (NotFoundException e) {
                isJakarta = false;
            }
        }
        return isJakarta;
    }

    public static Class<?> getBeanClass(Object bean) {
        return (Class<?>) ReflectionHelper.invoke(bean, bean.getClass(), "getBeanClass", null);
    }

    public static Class<? extends Annotation> getBeanScope(Object bean) {
        return (Class<? extends Annotation>) ReflectionHelper.invoke(bean, bean.getClass(), "getScope", null);
    }

    public static boolean isInExtraScope(Object bean) {
        Class<?> beanClass = getBeanClass(bean);
        for (HaCdiExtraContext extraContext: extraContexts.keySet()) {
            if (extraContext.containsBeanInstances(beanClass)) {
                return true;
            }
        }
        return false;
    }

    
    public static void transformContext(ClassPool classPool, CtClass ctClass) throws CannotCompileException, NotFoundException {
        addBeanRegistryToContext(classPool, ctClass);
        transformGet1(classPool, ctClass);
        transformGet2(classPool, ctClass);
        LOGGER.debug(ctClass.getName() + " - patched by bean registration.");
    }

    
    public static void addBeanRegistryToContext(ClassPool classPool, CtClass ctClass) throws CannotCompileException {
        CtField beanRegistryFld = CtField.make(
            "public static java.util.Map " + BEAN_REGISTRY_FIELD + ";" , ctClass
        );
        ctClass.addField(beanRegistryFld);
    }

    private static void transformGet1(ClassPool classPool, CtClass ctClass) throws CannotCompileException, NotFoundException {
        CtMethod methGet1;
        boolean isJakarta;
        if (isJakarta(classPool)) {
            methGet1 = ctClass.getDeclaredMethod("get", new CtClass[] {
                    classPool.get("jakarta.enterprise.context.spi.Contextual")
            });
            isJakarta = true;
        } else {
            methGet1 = ctClass.getDeclaredMethod("get", new CtClass[] {
                    classPool.get("javax.enterprise.context.spi.Contextual")
            });
            isJakarta = false;
        }

        if (methGet1 != null) {
            methGet1.insertAfter(getRegistrationCode(isJakarta));
        }
    }

    private static void transformGet2(ClassPool classPool, CtClass ctClass) throws CannotCompileException, NotFoundException {
        CtMethod methGet2;
        boolean isJakarta;
        if (isJakarta(classPool)) {
            methGet2 = ctClass.getDeclaredMethod("get", new CtClass[]{
                    classPool.get("jakarta.enterprise.context.spi.Contextual"),
                    classPool.get("jakarta.enterprise.context.spi.CreationalContext"),
            });
            isJakarta = true;
        } else {
            methGet2 = ctClass.getDeclaredMethod("get", new CtClass[]{
                    classPool.get("javax.enterprise.context.spi.Contextual"),
                    classPool.get("javax.enterprise.context.spi.CreationalContext"),
            });
            isJakarta = false;
        }
        if (methGet2 != null) {
            methGet2.insertAfter(getRegistrationCode(isJakarta));
        }
    }

    private static  String resolveJakartaPackage(boolean isJakarta) {
       return isJakarta ? "jakarta" : "javax";
    }

    private static String getRegistrationCode(boolean isJakarta) {
        String result =
            "if(" + BEAN_REGISTRY_FIELD + "==null){" +
                BEAN_REGISTRY_FIELD + "=new java.util.concurrent.ConcurrentHashMap();" +
            "}"+
            "org.hotswap.agent.plugin.cdi.HaCdiCommons.registerContextClass(this.getScope(),this.getClass());" +
            "if($_!=null && $1 instanceof " + resolveJakartaPackage(isJakarta) + ".enterprise.inject.spi.Bean){" +
                "String key=((" + resolveJakartaPackage(isJakarta) + ".enterprise.inject.spi.Bean) $1).getBeanClass().getName();" +
                "java.util.Map m=" + BEAN_REGISTRY_FIELD + ".get(key);" +
                "if(m==null) {" +
                    "synchronized(" + BEAN_REGISTRY_FIELD + "){" +
                        "m=" + BEAN_REGISTRY_FIELD + ".get(key);" +
                        "if(m==null) {" +
                            "m=java.util.Collections.synchronizedMap(new java.util.WeakHashMap());" +
                            BEAN_REGISTRY_FIELD + ".put(key,m);" +
                        "}" +
                    "}" +
                "}" +
                "m.put($_, java.lang.Boolean.TRUE);" +
            "}";
        return result;
    }

    
    @SuppressWarnings({ "rawtypes"})
    public static List<Object> getBeanInstances(Object bean) {
        List<Object> result = new ArrayList<>();
        Class<?> contextClass = getContextClass(getBeanScope(bean));
        if (contextClass != null) {
          Map beanRegistry = (Map) getBeanRegistry(contextClass);
          if (beanRegistry != null) {
              Map m = (Map) beanRegistry.get(getBeanClass(bean).getName());
              if (m != null) {
                  result.addAll(m.keySet());
              } else {
                  LOGGER.debug("BeanRegistry is empty for bean class '{}'", getBeanClass(bean).getName());
              }
          } else {
              LOGGER.error("BeanRegistry field not found in context class '{}'", contextClass.getName());
          }
        }
        for (HaCdiExtraContext extraContext: extraContexts.keySet()) {
            List<Object> instances = extraContext.getBeanInstances(getBeanClass(bean));
            if (instances != null) {
                result.addAll(instances);
            }
        }
        return result;
    }

    private static Object getBeanRegistry(Class<?> clazz) {
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(BEAN_REGISTRY_FIELD);
                return field.get(null);
            } catch (Exception e) {
                
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    
    public static void registerContextClass(Class<? extends Annotation> scope, Class<?> contextClass) {
        Map<Class<? extends Annotation>, Class<?>> currentScopeToContextMap = getCurrentScopeToContextMap();

        if (!currentScopeToContextMap.containsKey(scope)) {
            LOGGER.debug("Registering scope '{}' to scopeToContextMap@{}", scope.getName(), System.identityHashCode(currentScopeToContextMap));
            currentScopeToContextMap.put(scope, contextClass);
        }
    }

    
    public static Class<?> getContextClass(Class<? extends Annotation> scope) {
        return getCurrentScopeToContextMap().get(scope);
    }

    
    public static boolean isRegisteredScope(Class<? extends Annotation> scope) {
        return getContextClass(scope) != null;
    }

    @SuppressWarnings("unchecked")
    private static Map<Class<? extends Annotation>, Class<?>> getCurrentScopeToContextMap() {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        if (classLoader != null) {
            try {
                Class<?> clazz = classLoader.loadClass(HaCdiCommons.class.getName());
                if (clazz != HaCdiCommons.class) {
                    return (Map) ReflectionHelper.get(null, clazz, "scopeToContextMap");
                }
            } catch (Exception e) {
                LOGGER.error("getCurrentScopeToContextMap '{}' failed",  e.getMessage());
            }
        }
        return scopeToContextMap;
    }

    
    public static void registerExtraContext(HaCdiExtraContext extraContext) {
        extraContexts.put(extraContext, Boolean.TRUE);
    }

    
    public static void unregisterExtraContext(HaCdiExtraContext extraContext) {
        extraContexts.remove(extraContext);
    }

}
