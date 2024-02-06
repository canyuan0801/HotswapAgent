
package org.hotswap.agent.plugin.watchResources;

import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.config.PluginConfiguration;
import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.classloader.HotswapAgentClassLoaderExt;
import org.hotswap.agent.util.classloader.URLClassPathHelper;
import org.hotswap.agent.util.classloader.WatchResourcesClassLoader;
import org.hotswap.agent.watch.Watcher;

import java.net.URL;


@Plugin(name = "WatchResources", description = "Support for watchResources configuration property.",
        testedVersions = {"JDK 1.7.0_45"}, expectedVersions = {"JDK 1.6+"})
public class WatchResourcesPlugin {
    private static AgentLogger LOGGER = AgentLogger.getLogger(WatchResourcesPlugin.class);

    @Init
    Watcher watcher;

    @Init
    ClassLoader appClassLoader;



    WatchResourcesClassLoader watchResourcesClassLoader = new WatchResourcesClassLoader(false);


    @Init
    public static void init(PluginManager pluginManager, PluginConfiguration pluginConfiguration, ClassLoader appClassLoader) {
        LOGGER.debug("Init plugin at classLoader {}", appClassLoader);


        if (appClassLoader instanceof WatchResourcesClassLoader.UrlOnlyClassLoader)
            return;


        if (!pluginConfiguration.containsPropertyFile()) {
            LOGGER.debug("ClassLoader {} does not contain hotswap-agent.properties file, WatchResources skipped.", appClassLoader);
            return;
        }


        URL[] watchResources = pluginConfiguration.getWatchResources();
        if (watchResources.length == 0) {
            LOGGER.debug("ClassLoader {} has hotswap-agent.properties watchResources empty.", appClassLoader);
            return;
        }

        if (!URLClassPathHelper.isApplicable(appClassLoader) &&
                !(appClassLoader instanceof HotswapAgentClassLoaderExt)) {
            LOGGER.warning("Unable to modify application classloader. Classloader '{}' is of type '{}'," +
                            "unknown classloader type.\n" +
                            "*** watchResources configuration property will not be handled on JVM level ***",
                    appClassLoader, appClassLoader.getClass());
            return;
        }


        WatchResourcesPlugin plugin = (WatchResourcesPlugin) pluginManager.getPluginRegistry()
                .initializePlugin(WatchResourcesPlugin.class.getName(), appClassLoader);


        plugin.init(watchResources);
    }


    private void init(URL[] watchResources) {

        watchResourcesClassLoader.initWatchResources(watchResources, watcher);

        if (appClassLoader instanceof HotswapAgentClassLoaderExt) {
            ((HotswapAgentClassLoaderExt) appClassLoader).$$ha$setWatchResourceLoader(watchResourcesClassLoader);
        } else if (URLClassPathHelper.isApplicable(appClassLoader)) {

            URLClassPathHelper.setWatchResourceLoader(appClassLoader, watchResourcesClassLoader);
        }
    }
}
