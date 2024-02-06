
package org.hotswap.agent.util.classloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.LoaderClassPath;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.scanner.ClassPathScanner;
import org.hotswap.agent.util.scanner.Scanner;
import org.hotswap.agent.util.scanner.ScannerVisitor;


public class ClassLoaderDefineClassPatcher {

    private static AgentLogger LOGGER = AgentLogger.getLogger(ClassLoaderDefineClassPatcher.class);

    private static Map<String, List<byte[]>> pluginClassCache = new HashMap<>();


    public void patch(final ClassLoader classLoaderFrom, final String pluginPath,
                      final ClassLoader classLoaderTo, final ProtectionDomain protectionDomain) {

        List<byte[]> cache = getPluginCache(classLoaderFrom, pluginPath);

        if (cache != null) {

            final ClassPool cp = new ClassPool();
            cp.appendClassPath(new LoaderClassPath(getClass().getClassLoader()));
            Set<String> loadedClasses = new HashSet<>();
            String packagePrefix = pluginPath.replace('/', '.');

            for (byte[] pluginBytes: cache) {
                CtClass pluginClass = null;
                try {



                    InputStream is = new ByteArrayInputStream(pluginBytes);
                    pluginClass = cp.makeClass(is);
                    try {
                        classLoaderFrom.loadClass(pluginClass.getName());
                    } catch (NoClassDefFoundError e) {
                        LOGGER.trace("Skipping class loading {} in classloader {} - " +
                                "class has probably unresolvable dependency.", pluginClass.getName(), classLoaderTo);
                    }

                    transferTo(pluginClass, packagePrefix, classLoaderTo, protectionDomain, loadedClasses);
                } catch (CannotCompileException e) {
                    LOGGER.trace("Skipping class definition {} in app classloader {} - " +
                            "class is probably already defined.", pluginClass.getName(), classLoaderTo);
                } catch (NoClassDefFoundError e) {
                    LOGGER.trace("Skipping class definition {} in app classloader {} - " +
                            "class has probably unresolvable dependency.", pluginClass.getName(), classLoaderTo);
                } catch (Throwable e) {
                    LOGGER.trace("Skipping class definition app classloader {} - " +
                            "unknown error.", e, classLoaderTo);
                }
            }
        }

        LOGGER.debug("Classloader {} patched with plugin classes from agent classloader {}.", classLoaderTo, classLoaderFrom);

    }

    private void transferTo(CtClass pluginClass, String pluginPath, ClassLoader classLoaderTo,
                            ProtectionDomain protectionDomain, Set<String> loadedClasses) throws CannotCompileException {

        if (loadedClasses.contains(pluginClass.getName()) || pluginClass.isFrozen() ||
                !pluginClass.getName().startsWith(pluginPath)) {
            return;
        }

        try {
            if (!pluginClass.isInterface()) {
                CtClass[] ctClasses = pluginClass.getInterfaces();
                if (ctClasses != null && ctClasses.length > 0) {
                    for (CtClass ctClass : ctClasses) {
                        try {
                            transferTo(ctClass, pluginPath, classLoaderTo, protectionDomain, loadedClasses);
                        } catch (Throwable e) {
                            LOGGER.trace("Skipping class loading {} in classloader {} - " +
                                    "class has probably unresolvable dependency.", ctClass.getName(), classLoaderTo);
                        }
                    }
                }
            }
        } catch (NotFoundException e) {
        }

        try {
            CtClass ctClass = pluginClass.getSuperclass();
            if (ctClass != null) {
                try {
                    transferTo(ctClass, pluginPath, classLoaderTo, protectionDomain, loadedClasses);
                } catch (Throwable e) {
                    LOGGER.trace("Skipping class loading {} in classloader {} - " +
                            "class has probably unresolvable dependency.", ctClass.getName(), classLoaderTo);
                }
            }
        } catch (NotFoundException e) {
        }
        pluginClass.toClass(classLoaderTo, protectionDomain);
        loadedClasses.add(pluginClass.getName());
    }

    private List<byte[]> getPluginCache(final ClassLoader classLoaderFrom, final String pluginPath) {
        List<byte[]> ret = null;
        synchronized(pluginClassCache) {
            ret = pluginClassCache.get(pluginPath);
            if (ret == null) {
                final List<byte[]> retList = new ArrayList<>();
                Scanner scanner = new ClassPathScanner();
                try {
                    scanner.scan(classLoaderFrom, pluginPath, new ScannerVisitor() {
                        @Override
                        public void visit(InputStream file) throws IOException {









                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                            int readBytes;
                            byte[] data = new byte[16384];

                            while ((readBytes = file.read(data, 0, data.length)) != -1) {
                                buffer.write(data, 0, readBytes);
                            }

                            buffer.flush();
                            retList.add(buffer.toByteArray());
                        }

                    });
                } catch (IOException e) {
                    LOGGER.error("Exception while scanning 'org/hotswap/agent/plugin'", e);
                }
                ret = retList;
                pluginClassCache.put(pluginPath, ret);
            }
        }
        return ret;
    }


    public boolean isPatchAvailable(ClassLoader classLoader) {




        return classLoader != null &&
                !classLoader.getClass().getName().equals("sun.reflect.DelegatingClassLoader") &&
                !classLoader.getClass().getName().equals("jdk.internal.reflect.DelegatingClassLoader")
                ;
    }
}
