
package org.hotswap.agent.util.classloader;

import org.hotswap.agent.javassist.util.proxy.MethodFilter;
import org.hotswap.agent.javassist.util.proxy.MethodHandler;
import org.hotswap.agent.javassist.util.proxy.Proxy;
import org.hotswap.agent.javassist.util.proxy.ProxyFactory;
import org.hotswap.agent.javassist.util.proxy.ProxyObject;
import org.hotswap.agent.logging.AgentLogger;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlContext;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;


public class URLClassPathHelper {
    private static final AgentLogger LOGGER = AgentLogger.getLogger(URLClassPathHelper.class);

    private static Class<?> urlClassPathProxyClass = null;

    static {
        Class<?> urlClassPathClass = null;
        ClassLoader classLoader = URLClassPathHelper.class.getClassLoader();
        try {
            urlClassPathClass = classLoader.loadClass("sun.misc.URLClassPath");
        } catch (ClassNotFoundException e) {
            try {

                urlClassPathClass = classLoader.loadClass("jdk.internal.loader.URLClassPath");
            } catch (ClassNotFoundException e1) {
                LOGGER.error("Unable to loadClass URLClassPath!");
            }
        }
        if (urlClassPathClass != null) {
            ProxyFactory f = new ProxyFactory();
            f.setSuperclass(urlClassPathClass);
            f.setFilter(new MethodFilter() {
                public boolean isHandled(Method m) {
                    return !m.getName().equals("finalize");
                }
            });
            urlClassPathProxyClass = f.createClass();
        }
    }


    public static void prependClassPath(ClassLoader classLoader, URL[] extraClassPath) {
        synchronized (classLoader) {

            try {
                Field ucpField = getUcpField(classLoader);
                if (ucpField == null) {
                    LOGGER.debug("Unable to find ucp field in classLoader {}", classLoader);
                    return;
                }

                ucpField.setAccessible(true);

                URL[] origClassPath = getOrigClassPath(classLoader, ucpField);

                URL[] modifiedClassPath = new URL[origClassPath.length + extraClassPath.length];
                System.arraycopy(extraClassPath, 0, modifiedClassPath, 0, extraClassPath.length);
                System.arraycopy(origClassPath, 0, modifiedClassPath, extraClassPath.length, origClassPath.length);

                Object urlClassPath = createClassPathInstance(modifiedClassPath);

                ExtraURLClassPathMethodHandler methodHandler = new ExtraURLClassPathMethodHandler(modifiedClassPath);
                ((Proxy) urlClassPath).setHandler(methodHandler);

                setUcpFieldOfAllClassLoader(classLoader, ucpField, urlClassPath);

                LOGGER.debug("Added extraClassPath URLs {} to classLoader {}", Arrays.toString(extraClassPath), classLoader);
            } catch (Exception e) {
                LOGGER.error("Unable to add extraClassPath URLs {} to classLoader {}", e, Arrays.toString(extraClassPath), classLoader);
            }
        }
    }

    public static void setWatchResourceLoader(ClassLoader classLoader, final ClassLoader watchResourceLoader) {
        synchronized (classLoader) {

            try {
                Field ucpField = getUcpField(classLoader);
                if (ucpField == null) {
                    LOGGER.debug("Unable to find ucp field in classLoader {}", classLoader);
                    return;
                }

                ucpField.setAccessible(true);

                URL[] origClassPath = getOrigClassPath(classLoader, ucpField);
                Object urlClassPath = createClassPathInstance(origClassPath);

                ExtraURLClassPathMethodHandler methodHandler = new ExtraURLClassPathMethodHandler(origClassPath, watchResourceLoader);
                ((Proxy) urlClassPath).setHandler(methodHandler);

                setUcpFieldOfAllClassLoader(classLoader, ucpField, urlClassPath);

                LOGGER.debug("WatchResourceLoader registered to classLoader {}", classLoader);
            } catch (Exception e) {
                LOGGER.debug("Unable to register WatchResourceLoader to classLoader {}", e, classLoader);
            }
        }
    }

    private static Object createClassPathInstance(URL[] urls) throws Exception {
        try {

            Constructor<?> constr = urlClassPathProxyClass.getConstructor(new Class[]{URL[].class});
            return constr.newInstance(new Object[]{urls});
        } catch (NoSuchMethodException e) {

            Constructor<?> constr = urlClassPathProxyClass.getConstructor(new Class[]{URL[].class, AccessControlContext.class});
            return constr.newInstance(new Object[]{urls, null});
        }
    }

    @SuppressWarnings("unchecked")
    private static URL[] getOrigClassPath(ClassLoader classLoader, Field ucpField) throws IllegalAccessException,
            NoSuchFieldException {
        URL[] origClassPath = null;
        Object urlClassPath = ucpField.get(classLoader);

        if (urlClassPath instanceof ProxyObject) {
            ProxyObject p = (ProxyObject) urlClassPath;
            MethodHandler handler = p.getHandler();

            if (handler instanceof ExtraURLClassPathMethodHandler) {
                origClassPath = ((ExtraURLClassPathMethodHandler) handler).getOrigClassPath();
            }
        } else {
            if (classLoader instanceof URLClassLoader) {
                origClassPath = ((URLClassLoader) classLoader).getURLs();
            } else {
                Field pathField = ucpField.getType().getDeclaredField("path");
                pathField.setAccessible(true);
                List<URL> urls = (List<URL>) pathField.get(urlClassPath);
                origClassPath = urls.toArray(new URL[0]);
            }
        }
        return origClassPath;
    }


    private static Field getUcpField(ClassLoader classLoader) throws NoSuchFieldException {
        if (classLoader instanceof URLClassLoader) {
            return URLClassLoader.class.getDeclaredField("ucp");
        }

        Class<?> ucpOwner = classLoader.getClass();
        if (ucpOwner.getName().startsWith("jdk.internal.loader.ClassLoaders$")) {
            return ucpOwner.getDeclaredField("ucp");
        }

        return null;
    }


    private static void setUcpFieldOfAllClassLoader(ClassLoader classLoader, Field ucpField, Object urlClassPath) throws IllegalAccessException {

        ucpField.set(classLoader, urlClassPath);

        Class<?> currentClass = classLoader.getClass();
        while ((currentClass = currentClass.getSuperclass()) != null) {
            try {
                Field field = currentClass.getDeclaredField("ucp");
                field.setAccessible(true);
                field.set(classLoader, urlClassPath);
            } catch (NoSuchFieldException e) {
                break;
            }
        }
    }

    public static boolean isApplicable(ClassLoader classLoader) {
        if (classLoader == null) {
            return false;
        }

        if (classLoader instanceof URLClassLoader) {
            return true;
        }

        Class<?> ucpOwner = classLoader.getClass();
        return ucpOwner.getName().startsWith("jdk.internal.loader.ClassLoaders$");
    }

    public static class ExtraURLClassPathMethodHandler implements MethodHandler {

        private ClassLoader watchResourceLoader;
        URL[] origClassPath;

        public ExtraURLClassPathMethodHandler(URL[] origClassPath) {
            this.origClassPath = origClassPath;
        }

        public ExtraURLClassPathMethodHandler(URL[] origClassPath, ClassLoader watchResourceLoader) {
            this.origClassPath = origClassPath;
            this.watchResourceLoader = watchResourceLoader;
        }


        public URL[] getOrigClassPath() {
            return origClassPath;
        }



        public Object invoke(Object self, Method method, Method proceed, Object[] args) throws Throwable {
            String methodName = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();

            if ("findResource".equals(methodName) && parameterTypes.length == 2 &&
                    parameterTypes[0] == String.class &&
                    (parameterTypes[1] == Boolean.TYPE || parameterTypes[1] == Boolean.class)) {
                if (watchResourceLoader != null) {
                    URL resource = watchResourceLoader.getResource((String) args[0]);
                    if (resource != null) {
                        return resource;
                    }
                }
            } else if ("findResources".equals(methodName) && parameterTypes.length == 2 &&
                    parameterTypes[0] == String.class &&
                    (parameterTypes[1] == Boolean.TYPE || parameterTypes[1] == Boolean.class)) {
                if (watchResourceLoader != null) {
                    try {
                        Enumeration<URL> resources = watchResourceLoader.getResources((String) args[0]);
                        if (resources != null && resources.hasMoreElements()) {
                            return resources;
                        }
                    } catch (IOException e) {
                        LOGGER.debug("Unable to load resource {}", e, (String) args[0]);
                    }
                }
            }

            return proceed.invoke(self, args);
        }

    }

}
