
package org.hotswap.agent.plugin.spring.reload;

import org.hotswap.agent.command.MergeableCommand;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.logging.AgentLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;


public class YamlChangedCommand extends MergeableCommand {
    private static AgentLogger LOGGER = AgentLogger.getLogger(YamlChangedCommand.class);

    ClassLoader appClassLoader;

    URL url;
    Scheduler scheduler;

    public YamlChangedCommand(ClassLoader appClassLoader, URL url, Scheduler scheduler) {
        this.appClassLoader = appClassLoader;
        this.url = url;
        this.scheduler = scheduler;
    }

    @Override
    public void executeCommand() {
        try {
            Class<?> clazz = Class.forName("org.hotswap.agent.plugin.spring.reload.SpringChangedAgent", true, appClassLoader);
            Method method = clazz.getDeclaredMethod(
                    "addChangedYaml", new Class[]{URL.class});
            method.invoke(null, url);
        } catch (Exception e) {
            throw new RuntimeException("YamlChangedCommand.execute error", e);
        }
    }
}
