
package org.hotswap.agent.annotation.handler;

import org.hotswap.agent.config.ClassLoaderInitListener;
import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.config.PluginConfiguration;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.HotswapTransformer;
import org.hotswap.agent.watch.Watcher;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;


public class InitHandler implements PluginHandler<Init> {
    private static AgentLogger LOGGER = AgentLogger.getLogger(InitHandler.class);

    protected PluginManager pluginManager;

    public InitHandler(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public boolean initField(PluginAnnotation pluginAnnotation) {
        Field field = pluginAnnotation.getField();


        ClassLoader appClassLoader = pluginAnnotation.getPlugin() == null ? getClass().getClassLoader() :
                pluginManager.getPluginRegistry().getAppClassLoader(pluginAnnotation.getPlugin());

        Object value = resolveType(appClassLoader, pluginAnnotation.getPluginClass(), field.getType());
        field.setAccessible(true);
        try {
            field.set(pluginAnnotation.getPlugin(), value);
        } catch (IllegalAccessException e) {
            LOGGER.error("Unable to set plugin field '{}' to value '{}' on plugin '{}'",
                    e, field.getName(), value, pluginAnnotation.getPluginClass());
            return false;
        }

        return true;
    }



    @Override
    public boolean initMethod(PluginAnnotation pluginAnnotation) {
        Object plugin = pluginAnnotation.getPlugin();

        if (plugin == null) {

            if (Modifier.isStatic(pluginAnnotation.getMethod().getModifiers()))
                return registerClassLoaderInit(pluginAnnotation);
            else
                return true;
        } else {
            if (!Modifier.isStatic(pluginAnnotation.getMethod().getModifiers())) {
                ClassLoader appClassLoader = pluginManager.getPluginRegistry().getAppClassLoader(plugin);
                return invokeInitMethod(pluginAnnotation, plugin, appClassLoader);
            } else
                return true;
        }
    }


    private boolean invokeInitMethod(PluginAnnotation pluginAnnotation, Object plugin, ClassLoader classLoader) {
        List<Object> args = new ArrayList<>();
        for (Class type : pluginAnnotation.getMethod().getParameterTypes()) {
            args.add(resolveType(classLoader, pluginAnnotation.getPluginClass(), type));
        }
        try {
            pluginAnnotation.getMethod().invoke(plugin, args.toArray());
            return true;
        } catch (IllegalAccessException e) {
            LOGGER.error("IllegalAccessException in init method on plugin {}.", e, pluginAnnotation.getPluginClass());
            return false;
        } catch (InvocationTargetException e) {
            LOGGER.error("InvocationTargetException in init method on plugin {}.", e, pluginAnnotation.getPluginClass());
            return false;
        }
    }


    protected boolean registerClassLoaderInit(final PluginAnnotation pluginAnnotation) {
        LOGGER.debug("Registering ClassLoaderInitListener on {}", pluginAnnotation.getPluginClass());
        pluginManager.registerClassLoaderInitListener(new ClassLoaderInitListener() {
            @Override
            public void onInit(ClassLoader classLoader) {

                LOGGER.debug("Init plugin {} at classloader {}.", pluginAnnotation.getPluginClass(), classLoader);
                invokeInitMethod(pluginAnnotation, null, classLoader);
            }
        });
        return true;
    }


    @SuppressWarnings("unchecked")
    protected Object resolveType(ClassLoader classLoader, Class pluginClass, Class type) {

        if (type.isAssignableFrom(PluginManager.class)) {
            return pluginManager;
        } else if (type.isAssignableFrom(Watcher.class)) {
            return pluginManager.getWatcher();
        } else if (type.isAssignableFrom(Scheduler.class)) {
            return pluginManager.getScheduler();
        } else if (type.isAssignableFrom(HotswapTransformer.class)) {
            return pluginManager.getHotswapTransformer();
        } else if (type.isAssignableFrom(PluginConfiguration.class)) {
            return pluginManager.getPluginConfiguration(classLoader);
        } else if (type.isAssignableFrom(ClassLoader.class)) {
            return classLoader;
        } else if (type.isAssignableFrom(Instrumentation.class)) {
            return pluginManager.getInstrumentation();
        } else {
            LOGGER.error("Unable process @Init on plugin '{}'." +
                    " Type '" + type + "' is not recognized for @Init annotation.", pluginClass);
            return null;
        }
    }
}
