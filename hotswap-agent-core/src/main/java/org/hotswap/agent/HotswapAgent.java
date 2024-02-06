
package org.hotswap.agent;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.util.Version;

import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;


public class HotswapAgent {
    private static AgentLogger LOGGER = AgentLogger.getLogger(HotswapAgent.class);


      private static Set<String> disabledPlugins = new HashSet<>();


    private static boolean autoHotswap = false;


    private static String propertiesFilePath;

    public static void agentmain(String args, Instrumentation inst) {
        premain(args, inst);
    }

    public static void premain(String args, Instrumentation inst) {

        LOGGER.info("Loading Hotswap agent {{}} - unlimited runtime class redefinition.", Version.version());
        parseArgs(args);
        fixJboss7Modules();
        PluginManager.getInstance().init(inst);
        LOGGER.debug("Hotswap agent initialized.");

    }

    public static void parseArgs(String args) {
        if (args == null)
            return;

        for (String arg : args.split(",")) {
            String[] val = arg.split("=");
            if (val.length != 2) {
                LOGGER.warning("Invalid javaagent command line argument '{}'. Argument is ignored.", arg);
            }

            String option = val[0];
            String optionValue = val[1];

            if ("disablePlugin".equals(option)) {
                disabledPlugins.add(optionValue.toLowerCase());
            } else if ("autoHotswap".equals(option)) {
                autoHotswap = Boolean.valueOf(optionValue);
            } else if ("propertiesFilePath".equals(option)) {
                propertiesFilePath = optionValue;
            } else {
                LOGGER.warning("Invalid javaagent option '{}'. Argument '{}' is ignored.", option, arg);
            }
        }
    }


    public static String getExternalPropertiesFile() {
        return propertiesFilePath;
    }



    public static boolean isPluginDisabled(String pluginName) {
        return disabledPlugins.contains(pluginName.toLowerCase());
    }


    public static boolean isAutoHotswap() {
        return autoHotswap;
    }


    private static void fixJboss7Modules() {
        String JBOSS_SYSTEM_MODULES_KEY = "jboss.modules.system.pkgs";


        String oldValue = System.getProperty(JBOSS_SYSTEM_MODULES_KEY, null);
        System.setProperty(JBOSS_SYSTEM_MODULES_KEY, oldValue == null ? HOTSWAP_AGENT_EXPORT_PACKAGES : oldValue + "," + HOTSWAP_AGENT_EXPORT_PACKAGES);
    }

    public static final String HOTSWAP_AGENT_EXPORT_PACKAGES =
            "org.hotswap.agent.annotation,"
            + "org.hotswap.agent.command,"
            + "org.hotswap.agent.config,"
            + "org.hotswap.agent.logging,"
            + "org.hotswap.agent.plugin,"
            + "org.hotswap.agent.util,"
            + "org.hotswap.agent.watch,"
            + "org.hotswap.agent.versions,"
            + "org.hotswap.agent.javassist";
}
