
package org.hotswap.agent.plugin.mybatis;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.hotswap.agent.annotation.FileEvent;
import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.OnResourceFileEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.command.Command;
import org.hotswap.agent.command.ReflectionCommand;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.config.PluginConfiguration;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.mybatis.transformers.MyBatisTransformers;


@Plugin(name = "MyBatis",
        description = "Reload MyBatis configuration after configuration create/change.",
        testedVersions = {"All between 3.5.9"},
        expectedVersions = {"3.5.9"},
        supportClass = {MyBatisTransformers.class})
public class MyBatisPlugin {
    private static AgentLogger LOGGER = AgentLogger.getLogger(MyBatisPlugin.class);

    @Init
    Scheduler scheduler;

    @Init
    ClassLoader appClassLoader;

    Map<String, Object> configurationMap = new HashMap<>();

    Command reloadConfigurationCommand =
            new ReflectionCommand(this, MyBatisRefreshCommands.class.getName(), "reloadConfiguration");

    @Init
    public void init(PluginConfiguration pluginConfiguration) {
        LOGGER.info("MyBatis plugin initialized.");
    }

    public void registerConfigurationFile(String configFile, Object configObject) {
        if (configFile != null && !configurationMap.containsKey(configFile)) {
            LOGGER.debug("MyBatisPlugin - configuration file registered : {}", configFile);
            configurationMap.put(configFile, configObject);
        }
    }

    @OnResourceFileEvent(path = "/", filter = ".*.xml", events = {FileEvent.MODIFY})
    public void registerResourceListeners(URL url) throws URISyntaxException {
        if (configurationMap.containsKey(Paths.get(url.toURI()).toFile().getAbsolutePath())) {
            refresh(500);
        }
    }



    private void refresh(int timeout) {
        scheduler.scheduleCommand(reloadConfigurationCommand, timeout);
    }

}
