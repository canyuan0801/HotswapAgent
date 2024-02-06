
package org.hotswap.agent.config;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.command.impl.SchedulerImpl;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.HotswapTransformer;
import org.hotswap.agent.util.classloader.ClassLoaderDefineClassPatcher;
import org.hotswap.agent.watch.Watcher;
import org.hotswap.agent.watch.WatcherFactory;


public class PluginManager {
    private static AgentLogger LOGGER = AgentLogger.getLogger(PluginManager.class);

    public static final String PLUGIN_PACKAGE = "org.hotswap.agent.plugin";




    private static PluginManager INSTANCE = new PluginManager();


    public static PluginManager getInstance() {
        return INSTANCE;
    }


    private PluginManager() {
        hotswapTransformer = new HotswapTransformer();
        pluginRegistry = new PluginRegistry(this, classLoaderPatcher);
    }


    private Instrumentation instrumentation;

    private Object hotswapLock = new Object();




    public Object getPlugin(String clazz, ClassLoader classLoader) {
        try {
            return getPlugin(Class.forName(clazz), classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Plugin class not found " + clazz, e);
        }
    }


    public <T> T getPlugin(Class<T> clazz, ClassLoader classLoader) {
        return pluginRegistry.getPlugin(clazz, classLoader);
    }


    public boolean isPluginInitialized(String pluginClassName, ClassLoader classLoader) {
        Class<Object> pluginClass = pluginRegistry.getPluginClass(pluginClassName);
        return pluginClass != null && pluginRegistry.hasPlugin(pluginClass, classLoader, false);
    }


    public void init(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;


        ClassLoader classLoader = getClass().getClassLoader();
        classLoaderConfigurations.put(classLoader, new PluginConfiguration(classLoader));

        if (watcher == null) {
            try {
                watcher = new WatcherFactory().getWatcher();
            } catch (IOException e) {
                LOGGER.debug("Unable to create default watcher.", e);
            }
        }
        watcher.run();

        if (scheduler == null) {
            scheduler = new SchedulerImpl();
        }
        scheduler.run();

        pluginRegistry.scanPlugins(getClass().getClassLoader(), PLUGIN_PACKAGE);

        LOGGER.debug("Registering transformer ");
        instrumentation.addTransformer(hotswapTransformer);
    }

    ClassLoaderDefineClassPatcher classLoaderPatcher = new ClassLoaderDefineClassPatcher();
    Map<ClassLoader, PluginConfiguration> classLoaderConfigurations = new HashMap<>();
    Set<ClassLoaderInitListener> classLoaderInitListeners = new HashSet<>();

    public void registerClassLoaderInitListener(ClassLoaderInitListener classLoaderInitListener) {
        classLoaderInitListeners.add(classLoaderInitListener);


        classLoaderInitListener.onInit(getClass().getClassLoader());
    }

    public void initClassLoader(ClassLoader classLoader) {

        initClassLoader(classLoader, classLoader.getClass().getProtectionDomain());
    }

    public void initClassLoader(ClassLoader classLoader, ProtectionDomain protectionDomain) {

        if (classLoaderConfigurations.containsKey(classLoader))
            return;


        if (getClass().getClassLoader() != null &&
            classLoader != null &&
            classLoader.equals(getClass().getClassLoader().getParent()))
            return;



        synchronized (this) {
            if (classLoaderConfigurations.containsKey(classLoader))
                return;


            if (classLoader != null && classLoaderPatcher.isPatchAvailable(classLoader)) {
                classLoaderPatcher.patch(getClass().getClassLoader(), PLUGIN_PACKAGE.replace(".", "/"),
                        classLoader, protectionDomain);
            }


            PluginConfiguration pluginConfiguration = new PluginConfiguration(getPluginConfiguration(getClass().getClassLoader()), classLoader, false);
            classLoaderConfigurations.put(classLoader, pluginConfiguration);
            pluginConfiguration.init();
        }


        for (ClassLoaderInitListener classLoaderInitListener : classLoaderInitListeners)
            classLoaderInitListener.onInit(classLoader);
    }


    public void closeClassLoader(ClassLoader classLoader) {
        pluginRegistry.closeClassLoader(classLoader);
        classLoaderConfigurations.remove(classLoader);
        hotswapTransformer.closeClassLoader(classLoader);
    }


    public PluginConfiguration getPluginConfiguration(ClassLoader classLoader) {

        ClassLoader loader = classLoader;
        while (loader != null && !classLoaderConfigurations.containsKey(loader))
            loader = loader.getParent();

        return classLoaderConfigurations.get(loader);
    }



    private PluginRegistry pluginRegistry;


    public PluginRegistry getPluginRegistry() {
        return pluginRegistry;
    }


    public void setPluginRegistry(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    protected HotswapTransformer hotswapTransformer;


    public HotswapTransformer getHotswapTransformer() {
        return hotswapTransformer;
    }

    protected Watcher watcher;


    public Watcher getWatcher() {
        return watcher;
    }

    protected Scheduler scheduler;


    public Scheduler getScheduler() {
        return scheduler;
    }


    public void hotswap(Map<Class<?>, byte[]> reloadMap) {
        if (instrumentation == null) {
            throw new IllegalStateException("Plugin manager is not correctly initialized - no instrumentation available.");
        }

        synchronized (reloadMap) {
            ClassDefinition[] definitions = new ClassDefinition[reloadMap.size()];
            String[] classNames = new String[reloadMap.size()];
            int i = 0;
            for (Map.Entry<Class<?>, byte[]> entry : reloadMap.entrySet()) {
                classNames[i] = entry.getKey().getName();
                definitions[i++] = new ClassDefinition(entry.getKey(), entry.getValue());
            }
            try {
                LOGGER.reload("Reloading classes {} (autoHotswap)", Arrays.toString(classNames));
                synchronized (hotswapLock) {
                    instrumentation.redefineClasses(definitions);
                }
                LOGGER.debug("... reloaded classes {} (autoHotswap)", Arrays.toString(classNames));
            } catch (Exception e) {
                LOGGER.debug("... Fail to reload classes {} (autoHotswap), msg is {}", Arrays.toString(classNames), e);
                throw new IllegalStateException("Unable to redefine classes", e);
            }
            reloadMap.clear();
        }
    }


    public Instrumentation getInstrumentation() {
        return instrumentation;
    }


    public void scheduleHotswap(Map<Class<?>, byte[]> reloadMap, int timeout) {
        if (instrumentation == null) {
            throw new IllegalStateException("Plugin manager is not correctly initialized - no instrumentation available.");
        }
        getScheduler().scheduleCommand(new ScheduledHotswapCommand(reloadMap), timeout);
    }

}
