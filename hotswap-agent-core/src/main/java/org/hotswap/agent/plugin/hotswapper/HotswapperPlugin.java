
package org.hotswap.agent.plugin.hotswapper;

import org.hotswap.agent.HotswapAgent;
import org.hotswap.agent.annotation.FileEvent;
import org.hotswap.agent.annotation.OnClassFileEvent;
import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.command.Command;
import org.hotswap.agent.command.ReflectionCommand;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.config.PluginConfiguration;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.PluginManagerInvoker;
import org.hotswap.agent.util.classloader.*;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


@Plugin(name = "Hotswapper", description = "Watch for any class file change and reload (hotswap) it on the fly.",
        testedVersions = {"JDK 1.7.0_45"}, expectedVersions = {"JDK 1.6+"})
public class HotswapperPlugin {
    private static AgentLogger LOGGER = AgentLogger.getLogger(HotswapperPlugin.class);

    @Init
    Scheduler scheduler;

    @Init
    PluginManager pluginManager;


    final Map<Class<?>, byte[]> reloadMap = new HashMap<>();


    Command hotswapCommand;


    @OnClassFileEvent(classNameRegexp = ".*", events = {FileEvent.MODIFY, FileEvent.CREATE})
    public void watchReload(CtClass ctClass, ClassLoader appClassLoader, URL url) throws IOException, CannotCompileException {
        if (!ClassLoaderHelper.isClassLoaded(appClassLoader, ctClass.getName())) {
            LOGGER.trace("Class {} not loaded yet, no need for autoHotswap, skipped URL {}", ctClass.getName(), url);
            return;
        }

        LOGGER.debug("Class {} will be reloaded from URL {}", ctClass.getName(), url);


        Class clazz;
        try {
            clazz  = appClassLoader.loadClass(ctClass.getName());
        } catch (ClassNotFoundException e) {
            LOGGER.warning("Hotswapper tries to reload class {}, which is not known to application classLoader {}.",
                    ctClass.getName(), appClassLoader);
            return;
        }

        synchronized (reloadMap) {
            reloadMap.put(clazz, ctClass.toBytecode());
        }
        scheduler.scheduleCommand(hotswapCommand, 100, Scheduler.DuplicateSheduleBehaviour.SKIP);
    }


    public void initHotswapCommand(ClassLoader appClassLoader, String port) {
        if (port != null && port.length() > 0) {
            hotswapCommand = new ReflectionCommand(this, HotswapperCommand.class.getName(), "hotswap", appClassLoader,
                    port, reloadMap);
        } else {
            hotswapCommand = new Command() {
                @Override
                public void executeCommand() {
                    pluginManager.hotswap(reloadMap);
                }

                @Override
                public String toString() {
                    return "pluginManager.hotswap(" + Arrays.toString(reloadMap.keySet().toArray()) + ")";
                }
            };
        }
    }


    @Init
    public static void init(PluginConfiguration pluginConfiguration, ClassLoader appClassLoader) {

        if (appClassLoader == null) {
            LOGGER.debug("Bootstrap class loader is null, hotswapper skipped.");
            return;
        }

        LOGGER.debug("Init plugin at classLoader {}", appClassLoader);


        if (!HotswapAgent.isAutoHotswap() && !pluginConfiguration.containsPropertyFile()) {
            LOGGER.debug("ClassLoader {} does not contain hotswap-agent.properties file, hotswapper skipped.", appClassLoader);
            return;
        }


        if (!HotswapAgent.isAutoHotswap() && !pluginConfiguration.getPropertyBoolean("autoHotswap")) {
            LOGGER.debug("ClassLoader {} has autoHotswap disabled, hotswapper skipped.", appClassLoader);
            return;
        }


        String port = pluginConfiguration.getProperty("autoHotswap.port");

        HotswapperPlugin plugin = PluginManagerInvoker.callInitializePlugin(HotswapperPlugin.class, appClassLoader);
        if (plugin != null) {
            plugin.initHotswapCommand(appClassLoader, port);
        } else {
            LOGGER.debug("Hotswapper is disabled in {}", appClassLoader);
        }
    }
}
