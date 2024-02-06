
package org.hotswap.agent.plugin.owb;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.config.PluginConfiguration;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.owb.command.BeanClassRefreshCommand;
import org.hotswap.agent.plugin.owb.transformer.AbstractProducerTransformer;
import org.hotswap.agent.plugin.owb.transformer.BeansDeployerTransformer;
import org.hotswap.agent.plugin.owb.transformer.CdiContextsTransformer;
import org.hotswap.agent.plugin.owb.transformer.ProxyFactoryTransformer;
import org.hotswap.agent.util.AnnotationHelper;
import org.hotswap.agent.util.IOUtils;
import org.hotswap.agent.util.classloader.ClassLoaderHelper;
import org.hotswap.agent.util.signature.ClassSignatureComparerHelper;
import org.hotswap.agent.util.signature.ClassSignatureElement;
import org.hotswap.agent.watch.WatchEventListener;
import org.hotswap.agent.watch.WatchFileEvent;
import org.hotswap.agent.watch.Watcher;


@Plugin(name = "Owb",
        description = "OpenWebBeans framework(http:
        testedVersions = {"1.7.0-2.0.16"},
        expectedVersions = {"All between 1.7.0-2.0.16"},
        supportClass = { BeansDeployerTransformer.class, CdiContextsTransformer.class, ProxyFactoryTransformer.class, AbstractProducerTransformer.class })
public class OwbPlugin {

    private static AgentLogger LOGGER = AgentLogger.getLogger(OwbPlugin.class);

    private static final String VETOED_ANNOTATION = "javax.enterprise.inject.Vetoed";
    private static final String DS_EXCLUDED_ANNOTATION = "org.apache.deltaspike.core.api.exclude.Exclude";

    
    static boolean isTestEnvironment = false;
    
    static String archivePath = null;

    
    private static final int WAIT_ON_CREATE = 500;
    private static final int WAIT_ON_REDEFINE = 200;

    @Init
    Watcher watcher;

    @Init
    Scheduler scheduler;

    @Init
    ClassLoader appClassLoader;

    @Init
    PluginConfiguration pluginConfiguration;

    private boolean initialized = false;

    private BeanReloadStrategy beanReloadStrategy;

    private Map<URL, URL> registeredArchives = new HashMap<>();

    
    public void init() {
        if (!initialized) {
            LOGGER.info("OpenWebBeans plugin initialized.");
            initialized = true;
            beanReloadStrategy = setBeanReloadStrategy(pluginConfiguration.getProperty("owb.beanReloadStrategy"));
        }
    }

    private BeanReloadStrategy setBeanReloadStrategy(String property) {
        BeanReloadStrategy ret = BeanReloadStrategy.NEVER;
        if (property != null && !property.isEmpty()) {
            try {
                ret = BeanReloadStrategy.valueOf(property);
            } catch (Exception e) {
                LOGGER.error("Unknown property 'owb.beanReloadStrategy' value: {} ", property);
            }
        }
        return ret;
    }

    
    public void registerBeansXmls(Set bdaLocations) {

        
        for (final URL beanArchiveUrl : (Set<URL>) bdaLocations) {

            String beansXmlPath = beanArchiveUrl.getPath();

            if (!beansXmlPath.endsWith("beans.xml")) {
                LOGGER.debug("Skipping bda location: '{}' ", beanArchiveUrl);
                continue;
            }

            final String archivePath;

            if (beansXmlPath.endsWith("META-INF/beans.xml")) {
                archivePath = beansXmlPath.substring(0, beansXmlPath.length() - "META-INF/beans.xml".length());
            } else if (beansXmlPath.endsWith("WEB-INF/beans.xml")) {
                archivePath = beansXmlPath.substring(0, beansXmlPath.length() - "beans.xml".length()) + "classes";
            } else {
                LOGGER.warning("Unexpected beans.xml location '{}'", beansXmlPath);
                continue;
            }

            if (archivePath.endsWith(".jar!/")) {
                LOGGER.debug("Skipping unsupported jar beans.xml location '{}'", beansXmlPath);
                continue;
            }

            LOGGER.info("OWB: Registerering '{}' for changes....", archivePath);
            OwbPlugin.archivePath = archivePath; 

            try {
                URL archivePathUrl = resourceNameToURL(archivePath);

                if (registeredArchives.containsKey(archivePathUrl)) {
                    continue;
                }

                registeredArchives.put(archivePathUrl, beanArchiveUrl);

                URI uri = archivePathUrl.toURI();

                watcher.addEventListener(appClassLoader, uri, new WatchEventListener() {
                    @Override
                    public void onEvent(WatchFileEvent event) {
                        if (event.isFile() && event.getURI().toString().endsWith(".class")) {
                            
                            String className;
                            try {
                                className = IOUtils.urlToClassName(event.getURI());
                            } catch (IOException e) {
                                LOGGER.trace("Watch event on resource '{}' skipped, probably Ok because of delete/create event sequence (compilation not finished yet).",
                                        e, event.getURI());
                                return;
                            }
                            if (!ClassLoaderHelper.isClassLoaded(appClassLoader, className) || isTestEnvironment) {
                                
                                LOGGER.trace("register reload command: {} ", className);
                                scheduler.scheduleCommand(new BeanClassRefreshCommand(appClassLoader, archivePath, beanArchiveUrl, event), WAIT_ON_CREATE);
                            }
                        }
                    }
                });
                LOGGER.info("Registered  watch for path '{}' for changes.", archivePathUrl);
            } catch (URISyntaxException e) {
                LOGGER.error("Unable to watch path '{}' for changes.", e, archivePath);
            } catch (Exception e) {
                LOGGER.warning("registerBeanDeplArchivePath() exception : {}",  e.getMessage());
            }
        }
    }

    
    @OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE)
    public void classReload(ClassLoader classLoader, CtClass ctClass, Class<?> original) {
        if (classLoader != appClassLoader) {
            LOGGER.debug("Attempt to redefine class '{}' in unsupported classLoader{}.", original.getName(), classLoader);
            return;
        }
        if (original == null || isSyntheticCdiClass(ctClass.getName()) || isInnerNonPublicStaticClass(ctClass)) {
            if (original != null) {
                LOGGER.trace("Skipping synthetic or inner class {}.", original.getName());
            }
            return;
        }

        if (AnnotationHelper.hasAnnotation(ctClass, VETOED_ANNOTATION)) {
            LOGGER.trace("Skipping @Vetoed class {}.", ctClass.getName());
            return;
        }

        if (AnnotationHelper.hasAnnotation(ctClass, DS_EXCLUDED_ANNOTATION)) {
            LOGGER.trace("Skipping @Excluded class {}.", ctClass.getName());
            return;
        }

        try {
            String classUrl = ctClass.getURL().toExternalForm();
            Iterator<Entry<URL, URL>> iterator = registeredArchives.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<URL, URL> entry = iterator.next();
                if (classUrl.startsWith(entry.getKey().toExternalForm())) {
                    LOGGER.debug("Class '{}' redefined in classLoader {}.", original.getName(), classLoader);
                    String oldSignForProxyCheck = OwbClassSignatureHelper.getSignatureForProxyClass(original);
                    String oldSignByStrategy = OwbClassSignatureHelper.getSignatureByStrategy(beanReloadStrategy, original);
                    String oldFullSignature = ClassSignatureComparerHelper.getJavaClassSignature(original, ClassSignatureElement.values());
                    scheduler.scheduleCommand(
                            new BeanClassRefreshCommand(appClassLoader,
                                    original.getName(),
                                    oldFullSignature,
                                    oldSignForProxyCheck,
                                    oldSignByStrategy,
                                    entry.getValue(),
                                    beanReloadStrategy),
                            WAIT_ON_REDEFINE
                            );
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("classReload() exception {}.", e, e.getMessage());
        }
    }

    
    
    
    private boolean isSyntheticCdiClass(String className) {
        return className.contains("$$") || className.contains("$HibernateProxy$");
    }

    
    private boolean isInnerNonPublicStaticClass(CtClass ctClass) {
        try {
            CtClass declaringClass = ctClass.getDeclaringClass();
            if (declaringClass != null && (
                    (ctClass.getModifiers() & Modifier.STATIC) == 0 ||
                    (ctClass.getModifiers() & Modifier.PUBLIC) == 0)) {
                return true;
            }
        } catch (NotFoundException e) {
            
        }
        return false;
    }

    public URL resourceNameToURL(String resource) throws Exception {
        try {
            
            return new URL(resource);
        } catch (MalformedURLException e) {
            
            if (resource.startsWith("./"))
                resource = resource.substring(2);
            File file = new File(resource).getCanonicalFile();
            return file.toURI().toURL();
        }
    }
}
