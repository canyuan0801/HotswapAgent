
package org.hotswap.agent.plugin.spring;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.hotswap.agent.annotation.FileEvent;
import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.OnResourceFileEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.config.PluginConfiguration;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.spring.core.BeanDefinitionProcessor;
import org.hotswap.agent.plugin.spring.reload.ClassChangedCommand;
import org.hotswap.agent.plugin.spring.reload.PropertiesChangedCommand;
import org.hotswap.agent.plugin.spring.reload.SpringChangedReloadCommand;
import org.hotswap.agent.plugin.spring.reload.SpringReloadConfig;
import org.hotswap.agent.plugin.spring.reload.XmlsChangedCommand;
import org.hotswap.agent.plugin.spring.reload.YamlChangedCommand;
import org.hotswap.agent.plugin.spring.scanner.SpringBeanWatchEventListener;
import org.hotswap.agent.plugin.spring.transformers.BeanFactoryTransformer;
import org.hotswap.agent.plugin.spring.transformers.ClassPathBeanDefinitionScannerTransformer;
import org.hotswap.agent.plugin.spring.transformers.ConfigurationClassPostProcessorTransformer;
import org.hotswap.agent.plugin.spring.transformers.PlaceholderConfigurerSupportTransformer;
import org.hotswap.agent.plugin.spring.transformers.PostProcessorRegistrationDelegateTransformer;
import org.hotswap.agent.plugin.spring.transformers.ProxyReplacerTransformer;
import org.hotswap.agent.plugin.spring.transformers.ResourcePropertySourceTransformer;
import org.hotswap.agent.plugin.spring.transformers.XmlBeanDefinitionScannerTransformer;
import org.hotswap.agent.util.HotswapTransformer;
import org.hotswap.agent.util.IOUtils;
import org.hotswap.agent.util.PluginManagerInvoker;
import org.hotswap.agent.util.ReflectionHelper;
import org.hotswap.agent.watch.Watcher;


@Plugin(name = "Spring", description = "Reload Spring configuration after class definition/change.",
        testedVersions = {"All between 3.0.1 - 5.2.2"}, expectedVersions = {"3x", "4x", "5x"},
        supportClass = {ClassPathBeanDefinitionScannerTransformer.class,
                ProxyReplacerTransformer.class,
                ConfigurationClassPostProcessorTransformer.class,
                ResourcePropertySourceTransformer.class,
                PlaceholderConfigurerSupportTransformer.class,
                XmlBeanDefinitionScannerTransformer.class,
                PostProcessorRegistrationDelegateTransformer.class,
                BeanFactoryTransformer.class})
public class SpringPlugin {
    private static final AgentLogger LOGGER = AgentLogger.getLogger(SpringPlugin.class);

    public static String[] basePackagePrefixes;

    @Init
    HotswapTransformer hotswapTransformer;

    @Init
    Watcher watcher;

    @Init
    Scheduler scheduler;

    @Init
    ClassLoader appClassLoader;

    private Class springChangeHubClass;

    public void init() throws ClassNotFoundException {
        LOGGER.info("Spring plugin initialized");
        springChangeHubClass = Class.forName("org.hotswap.agent.plugin.spring.reload.SpringChangedAgent", true, appClassLoader);
        ReflectionHelper.set(null, springChangeHubClass, "appClassLoader", appClassLoader);
        this.registerBasePackageFromConfiguration();
        this.initBasePackagePrefixes();
    }

    public void init(String version) throws ClassNotFoundException {
        LOGGER.info("Spring plugin initialized - Spring core version '{}'", version);
        springChangeHubClass = Class.forName("org.hotswap.agent.plugin.spring.reload.SpringChangedAgent", true, appClassLoader);
        ReflectionHelper.set(null, springChangeHubClass, "appClassLoader", appClassLoader);
        this.registerBasePackageFromConfiguration();
        this.initBasePackagePrefixes();
    }

    private void initBasePackagePrefixes() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration(this.appClassLoader);
        if (basePackagePrefixes == null || basePackagePrefixes.length == 0) {
            basePackagePrefixes = pluginConfiguration.getBasePackagePrefixes();
        } else {
            String[] newBasePackagePrefixes = pluginConfiguration.getBasePackagePrefixes();
            List<String> both = new ArrayList<>(basePackagePrefixes.length + newBasePackagePrefixes.length);
            Collections.addAll(both, basePackagePrefixes);
            Collections.addAll(both, newBasePackagePrefixes);
            basePackagePrefixes = both.toArray(new String[both.size()]);
        }
    }

    @OnResourceFileEvent(path = "/", filter = ".*.xml", events = {FileEvent.MODIFY})
    public void registerResourceListeners(URL url) {
        scheduler.scheduleCommand(new XmlsChangedCommand(appClassLoader, url, scheduler));
        LOGGER.trace("Scheduling Spring reload for XML '{}'", url);
        scheduler.scheduleCommand(new SpringChangedReloadCommand(appClassLoader), SpringReloadConfig.reloadDelayMillis);
    }

    @OnResourceFileEvent(path = "/", filter = ".*.properties", events = {FileEvent.MODIFY})
    public void registerPropertiesListeners(URL url) {
        scheduler.scheduleCommand(new PropertiesChangedCommand(appClassLoader, url, scheduler));
        LOGGER.trace("Scheduling Spring reload for properties '{}'", url);
        scheduler.scheduleCommand(new SpringChangedReloadCommand(appClassLoader), SpringReloadConfig.reloadDelayMillis);
    }

    @OnResourceFileEvent(path = "/", filter = ".*.yaml", events = {FileEvent.MODIFY})
    public void registerYamlListeners(URL url) {
        scheduler.scheduleCommand(new YamlChangedCommand(appClassLoader, url, scheduler));

        LOGGER.trace("Scheduling Spring reload for yaml '{}'", url);
        scheduler.scheduleCommand(new SpringChangedReloadCommand(appClassLoader), SpringReloadConfig.reloadDelayMillis);
    }

    @OnClassLoadEvent(classNameRegexp = ".*", events = {LoadEvent.REDEFINE})
    public void registerClassListeners(Class<?> clazz) {
        scheduler.scheduleCommand(new ClassChangedCommand(appClassLoader, clazz, scheduler));
        LOGGER.trace("Scheduling Spring reload for class '{}' in classLoader {}", clazz, appClassLoader);
        scheduler.scheduleCommand(new SpringChangedReloadCommand(appClassLoader), SpringReloadConfig.reloadDelayMillis);
    }


    public void registerBasePackageFromConfiguration() {
        if (basePackagePrefixes != null) {
            for (String basePackagePrefix : basePackagePrefixes) {
                this.registerBasePackage(basePackagePrefix);
            }
        }
    }

    private void registerBasePackage(final String basePackage) {


        hotswapTransformer.registerTransformer(appClassLoader, getClassNameRegExp(basePackage),
                new SpringBeanClassFileTransformer(appClassLoader, scheduler, basePackage));
    }


    public void registerComponentScanBasePackage(final String basePackage) {
        LOGGER.info("Registering basePackage {}", basePackage);

        this.registerBasePackage(basePackage);

        Enumeration<URL> resourceUrls = null;
        try {
            resourceUrls = getResources(basePackage);
        } catch (IOException e) {
            LOGGER.error("Unable to resolve base package {} in classloader {}.", basePackage, appClassLoader);
            return;
        }


        while (resourceUrls.hasMoreElements()) {
            URL basePackageURL = resourceUrls.nextElement();

            if (!IOUtils.isFileURL(basePackageURL)) {
                LOGGER.debug("Spring basePackage '{}' - unable to watch files on URL '{}' for changes (JAR file?), limited hotswap reload support. " +
                        "Use extraClassPath configuration to locate class file on filesystem.", basePackage, basePackageURL);
            } else {
                watcher.addEventListener(appClassLoader, basePackageURL,
                        new SpringBeanWatchEventListener(scheduler, appClassLoader, basePackage));
            }
        }
    }

    private String getClassNameRegExp(String basePackage) {
        String regexp = basePackage;
        while (regexp.contains("**")) {
            regexp = regexp.replace("**", ".*");
        }
        if (!regexp.endsWith(".*")) {
            regexp += ".*";
        }
        return regexp;
    }

    private Enumeration<URL> getResources(String basePackage) throws IOException {
        String resourceName = basePackage;
        int index = resourceName.indexOf('*');
        if (index != -1) {
            resourceName = resourceName.substring(0, index);
            index = resourceName.lastIndexOf('.');
            if (index != -1) {
                resourceName = resourceName.substring(0, index);
            }
        }
        resourceName = resourceName.replace('.', '/');
        return appClassLoader.getResources(resourceName);
    }


    @OnClassLoadEvent(classNameRegexp = "org.springframework.beans.factory.support.DefaultListableBeanFactory")
    public static void register(ClassLoader appClassLoader, CtClass clazz, ClassPool classPool) throws NotFoundException, CannotCompileException {
        StringBuilder src = new StringBuilder("{");
        src.append("setCacheBeanMetadata(false);");

        src.append(PluginManagerInvoker.buildInitializePlugin(SpringPlugin.class));
        src.append(PluginManagerInvoker.buildCallPluginMethod(SpringPlugin.class, "init",
                "org.springframework.core.SpringVersion.getVersion()", String.class.getName()));
        src.append("}");

        for (CtConstructor constructor : clazz.getDeclaredConstructors()) {
            constructor.insertBeforeBody(src.toString());
            constructor.insertAfter("org.hotswap.agent.plugin.spring.reload.SpringChangedAgent.getInstance(this);");
        }












        CtMethod method = clazz.getDeclaredMethod("freezeConfiguration");
        method.insertBefore(
                "org.hotswap.agent.plugin.spring.core.ResetSpringStaticCaches.resetBeanNamesByType(this); " +
                        "setAllowRawInjectionDespiteWrapping(true); ");



        CtMethod registerBeanDefinitionMethod = clazz.getDeclaredMethod("registerBeanDefinition");
        registerBeanDefinitionMethod.insertBefore(BeanDefinitionProcessor.class.getName() + ".registerBeanDefinition(this, $1, $2);");

        CtMethod removeBeanDefinitionMethod = clazz.getDeclaredMethod("removeBeanDefinition");
        removeBeanDefinitionMethod.insertBefore(BeanDefinitionProcessor.class.getName() + ".removeBeanDefinition(this, $1);");
    }

    @OnClassLoadEvent(classNameRegexp = "org.springframework.aop.framework.CglibAopProxy")
    public static void cglibAopProxyDisableCache(CtClass ctClass) throws NotFoundException, CannotCompileException {
        CtMethod method = ctClass.getDeclaredMethod("createEnhancer");
        method.setBody("{" +
                "org.springframework.cglib.proxy.Enhancer enhancer = new org.springframework.cglib.proxy.Enhancer();" +
                "enhancer.setUseCache(false);" +
                "return enhancer;" +
                "}");

        LOGGER.debug("org.springframework.aop.framework.CglibAopProxy - cglib Enhancer cache disabled");
    }
}
