
package org.hotswap.agent.plugin.spring.getbean;

import org.hotswap.agent.javassist.*;
import org.hotswap.agent.logging.AgentLogger;
import org.springframework.core.SpringVersion;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.WeakHashMap;


public class EnhancerProxyCreater {

    private static AgentLogger LOGGER = AgentLogger.getLogger(EnhancerProxyCreater.class);
    private static EnhancerProxyCreater INSTANCE;
    public static final String SPRING_PACKAGE = "org.springframework.cglib.";
    public static final String CGLIB_PACKAGE = "net.sf.cglib.";

    private Class<?> springProxy;
    private Class<?> springCallback;
    private Class<?> springNamingPolicy;
    private Method createSpringProxy;
    private Class<?> cglibProxy;
    private Class<?> cglibCallback;
    private Class<?> cglibNamingPolicy;
    private Method createCglibProxy;

    private Object springLock = new Object();
    private Object cglibLock = new Object();
    private final ClassLoader loader;
    private final ProtectionDomain pd;

    final private Map<Object, Object> beanProxies = new WeakHashMap<>();

    public EnhancerProxyCreater(ClassLoader loader, ProtectionDomain pd) {
        super();
        this.loader = loader;
        this.pd = pd;
    }

    public static boolean isSupportedCglibProxy(Object bean) {
        if (bean == null) {
            return false;
        }
        String beanClassName = bean.getClass().getName();
        return beanClassName.contains("$$EnhancerBySpringCGLIB") || beanClassName.contains("$$EnhancerByCGLIB");
    }


    public static Object createProxy(Object beanFactry, Object bean, Class<?>[] paramClasses, Object[] paramValues) {
        if (INSTANCE == null) {
            INSTANCE = new EnhancerProxyCreater(bean.getClass().getClassLoader(), bean.getClass().getProtectionDomain());
        }
        return INSTANCE.create(beanFactry, bean, paramClasses, paramValues);
    }

    private Object create(Object beanFactry, Object bean, Class<?>[] paramClasses, Object[] paramValues) {
        Object proxyBean = null;
        if (beanProxies.containsKey(bean)) {
            proxyBean = beanProxies.get(bean);
        } else {
            synchronized (beanProxies) {
                if (beanProxies.containsKey(bean)) {
                    proxyBean = bean;
                } else {
                    proxyBean = doCreate(beanFactry, bean, paramClasses, paramValues);
                }
                beanProxies.put(bean, proxyBean);
            }
        }




        if (proxyBean instanceof SpringHotswapAgentProxy) {
            ((SpringHotswapAgentProxy) proxyBean).$$ha$setTarget(bean);
        }

        return proxyBean;
    }

    private Object doCreate(Object beanFactry, Object bean, Class<?>[] paramClasses, Object[] paramValues) {
        try {
            Method proxyCreater = getProxyCreationMethod(bean);
            if (proxyCreater == null) {
                return bean;
            } else {
                return proxyCreater.invoke(null, beanFactry, bean, paramClasses, paramValues);
            }
        } catch (IllegalArgumentException | InvocationTargetException e) {
            LOGGER.warning("Can't create proxy for " + bean.getClass().getSuperclass()
                    + " because there is no default constructor,"
                    + " which means your non-singleton bean created before won't get rewired with new props when update class.");
            return bean;
        } catch (IllegalAccessException | CannotCompileException | NotFoundException e) {
            LOGGER.error("Creating a proxy failed", e);
            throw new RuntimeException(e);
        }
    }

    private Method getProxyCreationMethod(Object bean) throws CannotCompileException, NotFoundException {
        if (getCp(loader).find("org.springframework.cglib.proxy.MethodInterceptor") != null) {
            if (createSpringProxy == null) {
                synchronized (springLock) {
                    if (createSpringProxy == null) {
                        ClassPool cp = getCp(loader);
                        springCallback = buildProxyCallbackClass(SPRING_PACKAGE, cp);
                        springNamingPolicy = buildNamingPolicyClass(SPRING_PACKAGE, cp);
                        springProxy = buildProxyCreaterClass(SPRING_PACKAGE, springCallback, springNamingPolicy, cp);
                        createSpringProxy = springProxy.getDeclaredMethods()[0];
                    }
                }
            }
            return createSpringProxy;
        } else if (getCp(loader).find("net.sf.cglib.proxy.MethodInterceptor") != null) {
            if (createCglibProxy == null) {
                synchronized (cglibLock) {
                    if (createCglibProxy == null) {
                        ClassPool cp = getCp(loader);
                        cglibCallback = buildProxyCallbackClass(CGLIB_PACKAGE, cp);
                        cglibNamingPolicy = buildNamingPolicyClass(CGLIB_PACKAGE, cp);
                        cglibProxy = buildProxyCreaterClass(CGLIB_PACKAGE, cglibCallback, cglibNamingPolicy, cp);
                        createCglibProxy = cglibProxy.getDeclaredMethods()[0];
                    }
                }
            }
            return createCglibProxy;
        } else {
            LOGGER.error("Unable to determine the location of the Cglib package");
            return null;
        }
    }


    private Class<?> buildProxyCreaterClass(String cglibPackage, Class<?> callback, Class<?> namingPolicy, ClassPool cp)
            throws CannotCompileException {
        CtClass ct = cp.makeClass("HotswapAgentSpringBeanProxy" + getClassSuffix(cglibPackage));
        String proxy = cglibPackage + "proxy.";
        String core = cglibPackage + "core.";
        String rawBody =
                "public static Object create(Object beanFactry, Object bean, Class[] classes, Object[] params) {" +
                        "{2} handler = new {2}(bean, beanFactry, classes, params);" +
                        "{0}Enhancer e = new {0}Enhancer();" +
                        "e.setUseCache(false);" +
                        "Class[] proxyInterfaces = new Class[bean.getClass().getInterfaces().length+1];" +
                        "Class[] classInterfaces = bean.getClass().getInterfaces();" +
                        "for (int i = 0; i < classInterfaces.length; i++) {" +
                            "proxyInterfaces[i] = classInterfaces[i];" +
                         "}" +
                         "proxyInterfaces[proxyInterfaces.length-1] = org.hotswap.agent.plugin.spring.getbean.SpringHotswapAgentProxy.class;" +
                         "e.setInterfaces(proxyInterfaces);" +
                         "e.setSuperclass(bean.getClass().getSuperclass());" +
                         "e.setNamingPolicy(new {3}());" +
                         "e.setCallbackType({2}.class);" +
                         tryObjenesisProxyCreation(cp) +
                         "e.setCallback(handler);" +
                         "return e.create();" +
                "}";
        String body = rawBody
                .replaceAll("\\{0\\}", proxy)
                .replaceAll("\\{1\\}", core)
                .replaceAll("\\{2\\}", callback.getName())
                .replaceAll("\\{3\\}", namingPolicy.getName());
        CtMethod m = CtNewMethod.make(body, ct);
        ct.addMethod(m);
        return ct.toClass(loader, pd);
    }







    private String tryObjenesisProxyCreation(ClassPool cp) {
        if (cp.find("org.springframework.objenesis.SpringObjenesis") == null) {
            return "";
        }


        if (SpringVersion.getVersion().startsWith("4.2.6") ||
                SpringVersion.getVersion().startsWith("4.3.0")) {
            return "";
        }

        return
                "org.springframework.objenesis.SpringObjenesis objenesis = new org.springframework.objenesis.SpringObjenesis();" +
                    "if (objenesis.isWorthTrying()) {" +

                            "Class proxyClass = e.createClass();" +
                            "Object proxyInstance = objenesis.newInstance(proxyClass, false);" +
                            "((org.springframework.cglib.proxy.Factory) proxyInstance).setCallbacks(new org.springframework.cglib.proxy.Callback[] {handler});" +
                            "return proxyInstance;" +


                    "}";
    }


    private Class<?> buildNamingPolicyClass(String cglibPackage, ClassPool cp) throws CannotCompileException, NotFoundException {
        CtClass ct = cp.makeClass("HotswapAgentSpringNamingPolicy" + getClassSuffix(cglibPackage));
        String core = cglibPackage + "core.";
        String originalNamingPolicy = core + "SpringNamingPolicy";
        if (cp.find(originalNamingPolicy) == null)
            originalNamingPolicy = core + "DefaultNamingPolicy";
        ct.setSuperclass(cp.get(originalNamingPolicy));
        String rawBody =
                "public String getClassName(String prefix, String source, Object key, {0}Predicate names) {" +
                        "return super.getClassName(prefix + \"$HOTSWAPAGENT_\", source, key, names);" +
                "}";
        String body = rawBody.replaceAll("\\{0\\}", core);
        CtMethod m = CtNewMethod.make(body, ct);
        ct.addMethod(m);
        return ct.toClass(loader, pd);
    }

    private static String getClassSuffix(String cglibPackage) {
        return String.valueOf(cglibPackage.hashCode()).replace("-", "_");
    }


    private Class<?> buildProxyCallbackClass(String cglibPackage, ClassPool cp) throws CannotCompileException,
            NotFoundException {
        String proxyPackage = cglibPackage + "proxy.";
        CtClass ct = cp.makeClass("HotswapSpringCallback" + getClassSuffix(cglibPackage));
        ct.setSuperclass(cp.get(DetachableBeanHolder.class.getName()));
        ct.addInterface(cp.get(proxyPackage + "MethodInterceptor"));

        String rawBody =
                "public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, {0}MethodProxy proxy) throws Throwable {" +
                    "if(method != null && method.getName().equals(\"finalize\") && method.getParameterTypes().length == 0) {" +
                        "return null;" +
                    "}" +
                    "if(method != null && method.getName().equals(\"$$ha$getTarget\")) {" +
                        "return getTarget();" +
                    "}" +
                    "if(method != null && method.getName().equals(\"$$ha$setTarget\")) {" +
                        "setTarget(args[0]); return null;" +
                    "}" +
                    "return proxy.invoke(getBean(), args);" +
                "}";
        String body = rawBody.replaceAll("\\{0\\}", proxyPackage);

        CtMethod m = CtNewMethod.make(body, ct);
        ct.addMethod(m);
        return ct.toClass(loader, pd);
    }

    private ClassPool getCp(ClassLoader loader) {
        ClassPool cp = new ClassPool();
        cp.appendSystemPath();
        cp.appendClassPath(new LoaderClassPath(loader));
        return cp;
    }


}