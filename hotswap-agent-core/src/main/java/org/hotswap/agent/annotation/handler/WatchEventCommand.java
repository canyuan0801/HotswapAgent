
package org.hotswap.agent.annotation.handler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hotswap.agent.annotation.FileEvent;
import org.hotswap.agent.command.MergeableCommand;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.LoaderClassPath;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.IOUtils;
import org.hotswap.agent.watch.WatchFileEvent;


public class WatchEventCommand<T extends Annotation> extends MergeableCommand {

    private static AgentLogger LOGGER = AgentLogger.getLogger(WatchEventCommand.class);

    private final PluginAnnotation<T> pluginAnnotation;
    private final WatchEventDTO watchEventDTO;
    private final WatchFileEvent event;
    private final ClassLoader classLoader;

    public static <T extends Annotation> WatchEventCommand<T> createCmdForEvent(PluginAnnotation<T> pluginAnnotation,
            WatchFileEvent event, ClassLoader classLoader) {
        WatchEventDTO watchEventDTO = WatchEventDTO.parse(pluginAnnotation.getAnnotation());


        if (!watchEventDTO.accept(event)) {
            return null;
        }


        if (watchEventDTO.isOnlyRegularFiles() && !event.isFile()) {
            LOGGER.trace("Skipping URI {} because it is not a regular file.", event.getURI());
            return null;
        }


        if (!Arrays.asList(watchEventDTO.getEvents()).contains(event.getEventType())) {
            LOGGER.trace("Skipping URI {} because it is not a requested event.", event.getURI());
            return null;
        }


        if (watchEventDTO.getFilter() != null && watchEventDTO.getFilter().length() > 0) {
            if (!event.getURI().toString().matches(watchEventDTO.getFilter())) {
                LOGGER.trace("Skipping URI {} because it does not match filter.", event.getURI(), watchEventDTO.getFilter());
                return null;
            }
        }
        return new WatchEventCommand<>(pluginAnnotation, event, classLoader, watchEventDTO);
    }

    private WatchEventCommand(PluginAnnotation<T> pluginAnnotation, WatchFileEvent event, ClassLoader classLoader, WatchEventDTO watchEventDTO) {
        this.pluginAnnotation = pluginAnnotation;
        this.event = event;
        this.classLoader = classLoader;
        this.watchEventDTO = watchEventDTO;
    }

    @Override
    public void executeCommand() {
        LOGGER.trace("Executing for pluginAnnotation={}, event={} at classloader {}", pluginAnnotation, event, classLoader);
        onWatchEvent(pluginAnnotation, event, classLoader);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WatchEventCommand that = (WatchEventCommand) o;

        if (classLoader != null ? !classLoader.equals(that.classLoader) : that.classLoader != null) return false;
        if (event != null ? !event.equals(that.event) : that.event != null) return false;
        if (pluginAnnotation != null ? !pluginAnnotation.equals(that.pluginAnnotation) : that.pluginAnnotation != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = pluginAnnotation != null ? pluginAnnotation.hashCode() : 0;
        result = 31 * result + (event != null ? event.hashCode() : 0);
        result = 31 * result + (classLoader != null ? classLoader.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "WatchEventCommand{" +
                "pluginAnnotation=" + pluginAnnotation +
                ", event=" + event +
                ", classLoader=" + classLoader +
                '}';
    }


    public void onWatchEvent(PluginAnnotation<T> pluginAnnotation, WatchFileEvent event, ClassLoader classLoader) {
        final T annot = pluginAnnotation.getAnnotation();
        Object plugin = pluginAnnotation.getPlugin();


        CtClass ctClass = null;


        if (watchEventDTO.isClassFileEvent()) {
            try {

                ctClass = createCtClass(event.getURI(), classLoader);
            } catch (Exception e) {
                LOGGER.error("Unable create CtClass for URI '{}'.", e, event.getURI());
                return;
            }


            if (ctClass == null || !ctClass.getName().matches(watchEventDTO.getClassNameRegexp()))
                return;
        }

        LOGGER.debug("Executing resource changed method {} on class {} for event {}",
                pluginAnnotation.getMethod().getName(), plugin.getClass().getName(), event);


        List<Object> args = new ArrayList<>();
        for (Class<?> type : pluginAnnotation.getMethod().getParameterTypes()) {
            if (type.isAssignableFrom(ClassLoader.class)) {
                args.add(classLoader);
            } else if (type.isAssignableFrom(URI.class)) {
                args.add(event.getURI());
            } else if (type.isAssignableFrom(URL.class)) {
                try {
                    args.add(event.getURI().toURL());
                } catch (MalformedURLException e) {
                    LOGGER.error("Unable to convert URI '{}' to URL.", e, event.getURI());
                    return;
                }
            } else if (type.isAssignableFrom(ClassPool.class)) {
                args.add(ClassPool.getDefault());
            } else if (type.isAssignableFrom(FileEvent.class)) {
                args.add(event.getEventType());
            } else if (watchEventDTO.isClassFileEvent() && type.isAssignableFrom(CtClass.class)) {
                args.add(ctClass);
            } else if (watchEventDTO.isClassFileEvent() && type.isAssignableFrom(String.class)) {
                args.add(ctClass != null ? ctClass.getName() : null);
            } else {
                LOGGER.error("Unable to call method {} on plugin {}. Method parameter type {} is not recognized.",
                        pluginAnnotation.getMethod().getName(), plugin.getClass().getName(), type);
                return;
            }
        }
        try {
            pluginAnnotation.getMethod().invoke(plugin, args.toArray());


            if (ctClass != null) {
                ctClass.detach();
            }
        } catch (IllegalAccessException e) {
            LOGGER.error("IllegalAccessException in method '{}' class '{}' classLoader '{}' on plugin '{}'",
                e, pluginAnnotation.getMethod().getName(), ctClass != null ? ctClass.getName() : "",
                classLoader != null ? classLoader.getClass().getName() : "", plugin.getClass().getName());
        } catch (InvocationTargetException e) {
            LOGGER.error("InvocationTargetException in method '{}' class '{}' classLoader '{}' on plugin '{}'",
                e, pluginAnnotation.getMethod().getName(), ctClass != null ? ctClass.getName() : "",
                classLoader != null ? classLoader.getClass().getName() : "", plugin.getClass().getName());
        }
    }



    private CtClass createCtClass(URI uri, ClassLoader classLoader) throws NotFoundException, IOException {
        File file = new File(uri);
        if (file.exists()) {
          ClassPool cp = new ClassPool();
          cp.appendClassPath(new LoaderClassPath(classLoader));
          return cp.makeClass(new ByteArrayInputStream(IOUtils.toByteArray(uri)));
        }
        return null;
    }
}
