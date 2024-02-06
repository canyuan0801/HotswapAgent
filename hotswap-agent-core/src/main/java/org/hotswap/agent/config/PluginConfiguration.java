
package org.hotswap.agent.config;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.hotswap.agent.HotswapAgent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.HotswapProperties;
import org.hotswap.agent.util.classloader.HotswapAgentClassLoaderExt;
import org.hotswap.agent.util.classloader.URLClassPathHelper;


public class PluginConfiguration {
    private static AgentLogger LOGGER = AgentLogger.getLogger(PluginConfiguration.class);

    private static final String PLUGIN_CONFIGURATION = "hotswap-agent.properties";


    private static final String INCLUDED_CLASS_LOADERS_KEY = "includedClassLoaderPatterns";


    private static final String EXCLUDED_CLASS_LOADERS_KEY = "excludedClassLoaderPatterns";

    Properties properties = new HotswapProperties();


    PluginConfiguration parent;


    final ClassLoader classLoader;


    URL configurationURL;


    boolean containsPropertyFileDirectly = false;


    public PluginConfiguration(ClassLoader classLoader) {
        this(null, classLoader);
    }

    public PluginConfiguration(PluginConfiguration parent, ClassLoader classLoader) {
        this(parent, classLoader, true);
    }

    public PluginConfiguration(PluginConfiguration parent, ClassLoader classLoader, boolean init) {
        this.parent = parent;
        this.classLoader = classLoader;

        loadConfigurationFile();
        if (init) {
            init();
        }
    }

    private void loadConfigurationFile() {

        try {
            String externalPropertiesFile = HotswapAgent.getExternalPropertiesFile();

            if (externalPropertiesFile != null) {
                configurationURL = resourceNameToURL(externalPropertiesFile);
                properties.load(configurationURL.openStream());
                return;
            }

        } catch (Exception e) {
            LOGGER.error("Error while loading external properties file " + configurationURL, e);
        }

        if (parent == null) {
            configurationURL = classLoader == null
                    ? ClassLoader.getSystemResource(PLUGIN_CONFIGURATION)
                    : classLoader.getResource(PLUGIN_CONFIGURATION);

            try {
                if (configurationURL != null) {
                    containsPropertyFileDirectly = true;
                    properties.load(configurationURL.openStream());

                }


                System.getProperties().forEach((key, value) -> properties.put(key, value));

            } catch (Exception e) {
                LOGGER.error("Error while loading 'hotswap-agent.properties' from base URL " + configurationURL, e);
            }

        } else {


            try {
                Enumeration<URL> urls = null;

                if (classLoader != null) {
                    urls = classLoader.getResources(PLUGIN_CONFIGURATION);
                }

                if (urls == null) {
                    urls = ClassLoader.getSystemResources(PLUGIN_CONFIGURATION);
                }

                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();

                    boolean found = false;

                    ClassLoader parentClassLoader = parent.getClassLoader();
                    Enumeration<URL> parentUrls = parentClassLoader == null
                            ? ClassLoader.getSystemResources(PLUGIN_CONFIGURATION)
                            : parentClassLoader.getResources(PLUGIN_CONFIGURATION);

                    while (parentUrls.hasMoreElements()) {
                        if (url.equals(parentUrls.nextElement()))
                            found = true;
                    }

                    if (!found) {
                        configurationURL = url;
                        break;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error while loading 'hotswap-agent.properties' from URL " + configurationURL, e);
            }

            if (configurationURL == null) {
                configurationURL = parent.configurationURL;
                LOGGER.debug("Classloader does not contain 'hotswap-agent.properties', using parent file '{}'"
                        , parent.configurationURL);

            } else {
                LOGGER.debug("Classloader contains 'hotswap-agent.properties' at location '{}'", configurationURL);
                containsPropertyFileDirectly = true;
            }
            try {
                if (configurationURL != null)
                    properties.load(configurationURL.openStream());
            } catch (Exception e) {
                LOGGER.error("Error while loading 'hotswap-agent.properties' from URL " + configurationURL, e);
            }
        }
    }



    protected void init() {
        LogConfigurationHelper.configureLog(properties);
        initPluginPackage();
        checkProperties();
        initIncludedClassLoaderPatterns();
        initExcludedClassLoaderPatterns();
        initExtraClassPath();
    }

    private void initPluginPackage() {

        if (properties.containsKey("pluginPackages")) {
            String pluginPackages = properties.getProperty("pluginPackages");

            for (String pluginPackage : pluginPackages.split(",")) {
                PluginManager.getInstance().getPluginRegistry().scanPlugins(getClassLoader(), pluginPackage);
            }
        }
    }

    private void checkProperties(){
        if (properties != null && properties.containsKey(INCLUDED_CLASS_LOADERS_KEY) &&
                properties.containsKey(EXCLUDED_CLASS_LOADERS_KEY)) {
            throw new IllegalArgumentException("includedClassLoaderPatterns, excludedClassLoaderPatterns in" +
                    "hotswap-agent.properties are exclusive to each other. You cannot configure both options");
        }
    }

    private void initExtraClassPath() {
        URL[] extraClassPath = getExtraClasspath();
        if (extraClassPath.length > 0 && !checkExcluded()) {
            if (classLoader instanceof HotswapAgentClassLoaderExt) {
                ((HotswapAgentClassLoaderExt) classLoader).$$ha$setExtraClassPath(extraClassPath);
            } else if (URLClassPathHelper.isApplicable(classLoader)) {
                URLClassPathHelper.prependClassPath(classLoader, extraClassPath);
            } else {
                LOGGER.debug("Unable to set extraClasspath to {} on classLoader {}. Only classLoader with 'ucp' " +
                                "field present is supported.\n*** extraClasspath configuration property will not be " +
                                "handled on JVM level ***", Arrays.toString(extraClassPath), classLoader);
            }
        }
    }

    private void initIncludedClassLoaderPatterns() {
        if (properties != null && properties.containsKey(INCLUDED_CLASS_LOADERS_KEY)) {
            List<Pattern> includedClassLoaderPatterns = new ArrayList<>();
            for (String pattern : properties.getProperty(INCLUDED_CLASS_LOADERS_KEY).split(",")) {
                includedClassLoaderPatterns.add(Pattern.compile(pattern));
            }
            PluginManager.getInstance().getHotswapTransformer()
                    .setIncludedClassLoaderPatterns(includedClassLoaderPatterns);
        }
    }

    private void initExcludedClassLoaderPatterns() {
        if (properties != null && properties.containsKey(EXCLUDED_CLASS_LOADERS_KEY)) {
            List<Pattern> excludedClassLoaderPatterns = new ArrayList<>();
            for (String pattern : properties.getProperty(EXCLUDED_CLASS_LOADERS_KEY).split(",")) {
                excludedClassLoaderPatterns.add(Pattern.compile(pattern));
            }

            PluginManager.getInstance().getHotswapTransformer()
                    .setExcludedClassLoaderPatterns(excludedClassLoaderPatterns);
        }
    }

    private boolean checkExcluded() {
        if (PluginManager.getInstance().getHotswapTransformer().getIncludedClassLoaderPatterns() != null) {
            for (Pattern pattern : PluginManager.getInstance().getHotswapTransformer().getIncludedClassLoaderPatterns()) {
                if (pattern.matcher(classLoader.getClass().getName()).matches()) {
                    return false;
                }
            }
            return true;
        }

        if (PluginManager.getInstance().getHotswapTransformer().getExcludedClassLoaderPatterns() != null) {
            for (Pattern pattern : PluginManager.getInstance().getHotswapTransformer().getExcludedClassLoaderPatterns()) {
                if (pattern.matcher(classLoader.getClass().getName()).matches()) {
                    return true;
                }
            }
        }
        return false;
    }


    public String getProperty(String property) {
        if (properties.containsKey(property))
            return properties.getProperty(property);
        else if (parent != null)
            return parent.getProperty(property);
        else
            return null;
    }


    public String getProperty(String property, String defaultValue) {
        String value = getProperty(property);
        return value != null ? value : defaultValue;
    }


    public boolean getPropertyBoolean(String property) {
        if (properties.containsKey(property))
            return Boolean.valueOf(properties.getProperty(property));
        else if (parent != null)
            return parent.getPropertyBoolean(property);
        else
            return false;
    }


    public URL[] getExtraClasspath() {
        return convertToURL(getProperty("extraClasspath"));
    }


    public URL[] getWatchResources() {
        return convertToURL(getProperty("watchResources"));
    }


    public String[] getBasePackagePrefixes() {
        String basePackagePrefix = getProperty("spring.basePackagePrefix");
        if (basePackagePrefix != null) {
            return basePackagePrefix.split(",");
        }
        return null;
    }


    public URL[] getWebappDir() {
        return convertToURL(getProperty("webappDir"));
    }


    public List<String> getDisabledPlugins() {
        List<String> ret = new ArrayList<>();
        for (String disabledPlugin : getProperty("disabledPlugins", "").split(",")) {
            ret.add(disabledPlugin.trim());
        }
        return ret;
    }


    public boolean isDisabledPlugin(String pluginName) {
        return HotswapAgent.isPluginDisabled(pluginName) || getDisabledPlugins().contains(pluginName);
    }


    public boolean isDisabledPlugin(Class<?> pluginClass) {
        Plugin pluginAnnotation = pluginClass.getAnnotation(Plugin.class);
        return isDisabledPlugin(pluginAnnotation.name());
    }



    private URL[] convertToURL(String resources) {
        List<URL> ret = new ArrayList<>();

        if (resources != null) {
            StringTokenizer tokenizer = new StringTokenizer(resources, ",;");
            while (tokenizer.hasMoreTokens()) {
                String name = tokenizer.nextToken().trim();
                try {
                    ret.add(resourceNameToURL(name));
                } catch (Exception e) {
                    LOGGER.error("Invalid configuration value: '{}' is not a valid URL or path and will be skipped.", name, e);
                }
            }
        }

        return ret.toArray(new URL[ret.size()]);
    }

    private static URL resourceNameToURL(String resource) throws Exception {
        try {

            return new URL(resource);
        } catch (MalformedURLException e) {

            if (resource.startsWith("./"))
                resource = resource.substring(2);

            File file = new File(resource).getCanonicalFile();
            return file.toURI().toURL();
        }
    }


    public ClassLoader getClassLoader() {
        return classLoader;
    }


    public boolean containsPropertyFile() {
        return containsPropertyFileDirectly;
    }
}
