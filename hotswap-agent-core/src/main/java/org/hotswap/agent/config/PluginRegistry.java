
package org.hotswap.agent.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hotswap.agent.HotswapAgent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.annotation.handler.AnnotationProcessor;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.classloader.ClassLoaderDefineClassPatcher;
import org.hotswap.agent.util.scanner.ClassPathAnnotationScanner;
import org.hotswap.agent.util.scanner.ClassPathScanner;


public class PluginRegistry {

    private static AgentLogger LOGGER = AgentLogger.getLogger(PluginRegistry.class);


    protected Map<Class, Map<ClassLoader, Object>> registeredPlugins = Collections.synchronizedMap(new HashMap<Class, Map<ClassLoader, Object>>());


    public Map<Class, Map<ClassLoader, Object>> getRegisteredPlugins() {
        return registeredPlugins;
    }


    private PluginManager pluginManager;


    private ClassPathAnnotationScanner annotationScanner;

    public void setAnnotationScanner(ClassPathAnnotationScanner annotationScanner) {
        this.annotationScanner = annotationScanner;
    }


    protected AnnotationProcessor annotationProcessor;

    public void setAnnotationProcessor(AnnotationProcessor annotationProcessor) {
        this.annotationProcessor = annotationProcessor;
    }


    private ClassLoaderDefineClassPatcher classLoaderPatcher;

    public void setClassLoaderPatcher(ClassLoaderDefineClassPatcher classLoaderPatcher) {
        this.classLoaderPatcher = classLoaderPatcher;
    }


    public PluginRegistry(PluginManager pluginManager, ClassLoaderDefineClassPatcher classLoaderPatcher) {
        this.pluginManager = pluginManager;
        this.classLoaderPatcher = classLoaderPatcher;
        annotationScanner = new ClassPathAnnotationScanner(Plugin.class.getName(), new ClassPathScanner());
        annotationProcessor = new AnnotationProcessor(pluginManager);
    }


    public void scanPlugins(ClassLoader classLoader, String pluginPackage) {
        String pluginPath = pluginPackage.replace(".", "/");
        ClassLoader agentClassLoader = getClass().getClassLoader();

        try {
            List<String> discoveredPlugins = annotationScanner.scanPlugins(classLoader, pluginPath);
            List<String> discoveredPluginNames = new ArrayList<>();



            if (discoveredPlugins.size() > 0 && agentClassLoader != classLoader) {
                classLoaderPatcher.patch(classLoader, pluginPath, agentClassLoader, null);
            }

            for (String discoveredPlugin : discoveredPlugins) {
                Class pluginClass = Class.forName(discoveredPlugin, true, agentClassLoader);
                Plugin pluginAnnotation = (Plugin) pluginClass.getAnnotation(Plugin.class);

                if (pluginAnnotation == null) {
                    LOGGER.error("Scanner discovered plugin class {} which does not contain @Plugin annotation.", pluginClass);
                    continue;
                }
                String pluginName = pluginAnnotation.name();

                if (HotswapAgent.isPluginDisabled(pluginName)) {
                    LOGGER.debug("Plugin {} is disabled, skipping...", pluginName);
                    continue;
                }



                if (registeredPlugins.containsKey(pluginClass))
                    continue;

                registeredPlugins.put(pluginClass, Collections.synchronizedMap(new HashMap<ClassLoader, Object>()));

                if (annotationProcessor.processAnnotations(pluginClass, pluginClass)) {
                    LOGGER.debug("Plugin registered {}.", pluginClass);
                } else {
                    LOGGER.error("Error processing annotations for plugin {}. Plugin was unregistered.", pluginClass);
                    registeredPlugins.remove(pluginClass);
                }

                discoveredPluginNames.add(pluginName);
            }

            LOGGER.info("Discovered plugins: " + Arrays.toString(discoveredPluginNames.toArray()));

        } catch (Exception e) {
            LOGGER.error("Error in plugin initial processing for plugin package '{}'", e, pluginPackage);
        }
    }


    public Object initializePlugin(String pluginClass, ClassLoader appClassLoader) {
        if (appClassLoader == null)
            throw new IllegalArgumentException("Cannot initialize plugin '" + pluginClass + "', appClassLoader is null.");


        pluginManager.initClassLoader(appClassLoader);

        Class<Object> clazz = getPluginClass(pluginClass);


        if (pluginManager.getPluginConfiguration(appClassLoader).isDisabledPlugin(clazz)) {
            LOGGER.debug("Plugin {} disabled in classloader {}.", clazz, appClassLoader );
            return null;
        }


        if (doHasPlugin(clazz, appClassLoader, false, true)) {
            LOGGER.debug("Plugin {} already initialized in parent classloader of {}.", clazz, appClassLoader );
            return getPlugin(clazz, appClassLoader);
        }

        Object pluginInstance = registeredPlugins.get(clazz).get(appClassLoader);

        if (annotationProcessor.processAnnotations(pluginInstance)) {
            LOGGER.info("Plugin '{}' initialized in ClassLoader '{}'.", pluginClass, appClassLoader);
        } else {
            LOGGER.error("Plugin '{}' NOT initialized in ClassLoader '{}', error while processing annotations.", pluginClass, appClassLoader);
            registeredPlugins.get(clazz).remove(appClassLoader);
        }

        return pluginInstance;
    }

    public void initializePluginInstance(Object pluginInstance) {
        registeredPlugins.put(pluginInstance.getClass(),
                Collections.singletonMap(pluginInstance.getClass().getClassLoader(), pluginInstance));
        if (!annotationProcessor.processAnnotations(pluginInstance)) {
            throw new IllegalStateException("Unable to initialize plugin");
        }

    }


    public <T> T getPlugin(Class<T> pluginClass, ClassLoader classLoader) {
        if (registeredPlugins.isEmpty()) {
            throw new IllegalStateException("No plugin initialized. " +
                    "The Hotswap Agent JAR must NOT be in app classloader (only registered as --javaagent: startup parameter). " +
                    "Please check your mapPreviousState.");
        }

        if (!registeredPlugins.containsKey(pluginClass))
            throw new IllegalArgumentException(String.format("Plugin %s is not known to the registry.", pluginClass));

        Map<ClassLoader, Object> pluginInstances = registeredPlugins.get(pluginClass);
        synchronized(pluginInstances) {
            for (Map.Entry<ClassLoader, Object> registeredClassLoaderEntry : pluginInstances.entrySet()) {
                if (isParentClassLoader(registeredClassLoaderEntry.getKey(), classLoader)) {

                    return (T) registeredClassLoaderEntry.getValue();
                }
            }
        }


        throw new IllegalArgumentException(String.format("Plugin %s is not initialized in classloader %s.", pluginClass, classLoader));
    }


    public boolean hasPlugin(Class<?> pluginClass, ClassLoader classLoader, boolean checkParent) {
        return doHasPlugin(pluginClass, classLoader,checkParent, false);
    }

    public boolean doHasPlugin(Class<?> pluginClass, ClassLoader classLoader, boolean checkParent, boolean createIfMissing) {
        if (!registeredPlugins.containsKey(pluginClass))
            return false;

        Map<ClassLoader, Object> pluginInstances = registeredPlugins.get(pluginClass);
        synchronized (pluginInstances) {
            for (Map.Entry<ClassLoader, Object> registeredClassLoaderEntry : pluginInstances.entrySet()) {
                if (checkParent && isParentClassLoader(registeredClassLoaderEntry.getKey(), classLoader)) {
                    return true;
                } else if (registeredClassLoaderEntry.getKey().equals(classLoader)) {
                    return true;
                }
            }
            if (createIfMissing) {
                Object pluginInstance = instantiate((Class<Object>) pluginClass);
                pluginInstances.put(classLoader, pluginInstance);
            }
        }
        return false;
    }


    public ClassLoader getAppClassLoader(Object plugin) {

        Class<Object> clazz = getPluginClass(plugin.getClass().getName());
        Map<ClassLoader, Object> pluginInstances = registeredPlugins.get(clazz);
        if (pluginInstances != null) {
            synchronized(pluginInstances) {
                for (Map.Entry<ClassLoader, Object> entry : pluginInstances.entrySet()) {
                    if (entry.getValue().equals(plugin))
                        return entry.getKey();
                }
            }
        }
        throw new IllegalArgumentException("Plugin not found in the registry " + plugin);
    }




    protected Class<Object> getPluginClass(String pluginClass) {
        try {

            if (getClass().getClassLoader() == null) {
                return (Class<Object>) Class.forName(pluginClass, true, null);
            }
            return (Class<Object>) getClass().getClassLoader().loadClass(pluginClass);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Plugin class not found " + pluginClass, e);
        }
    }


    private boolean isParentClassLoader(ClassLoader parentClassLoader, ClassLoader classLoader) {
        if (parentClassLoader.equals(classLoader))
            return true;
        else if (classLoader.getParent() != null)
            return isParentClassLoader(parentClassLoader, classLoader.getParent());
        else
            return false;
    }


    protected Object instantiate(Class<Object> plugin) {
        try {
            return plugin.newInstance();
        } catch (InstantiationException e) {
            LOGGER.error("Error instantiating plugin: " + plugin.getClass().getName(), e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Plugin: " + plugin.getClass().getName()
                    + " does not contain public no param constructor", e);
        }
        return null;
    }


    public void closeClassLoader(ClassLoader classLoader) {
        LOGGER.debug("Closing classloader {}.", classLoader);
        synchronized (registeredPlugins) {
            for (Map<ClassLoader, Object> plugins : registeredPlugins.values()) {
                plugins.remove(classLoader);
            }
        }
    }
}
