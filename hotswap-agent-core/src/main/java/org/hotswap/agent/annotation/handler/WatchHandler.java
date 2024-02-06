
package org.hotswap.agent.annotation.handler;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;

import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.watch.WatchEventListener;
import org.hotswap.agent.watch.WatchFileEvent;


public class WatchHandler<T extends Annotation> implements PluginHandler<T> {
    private static AgentLogger LOGGER = AgentLogger.getLogger(WatchHandler.class);

    protected PluginManager pluginManager;

    public WatchHandler(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public boolean initField(PluginAnnotation<T> pluginAnnotation) {
        throw new IllegalAccessError("@OnResourceFileEvent annotation not allowed on fields.");
    }


    @Override
    public boolean initMethod(final PluginAnnotation<T> pluginAnnotation) {
        LOGGER.debug("Init for method " + pluginAnnotation.getMethod());

        ClassLoader classLoader = pluginManager.getPluginRegistry().getAppClassLoader(pluginAnnotation.getPlugin());

        try {
            registerResources(pluginAnnotation, classLoader);
        } catch (IOException e) {
            LOGGER.error("Unable to register resources for annotation {} on method {} class {}", e,
                    pluginAnnotation.getAnnotation(),
                    pluginAnnotation.getMethod().getName(),
                    pluginAnnotation.getMethod().getDeclaringClass().getName());
            return false;
        }

        return true;
    }


    private void registerResources(final PluginAnnotation<T> pluginAnnotation, final ClassLoader classLoader) throws IOException {
        final T annot = pluginAnnotation.getAnnotation();
        WatchEventDTO watchEventDTO =  WatchEventDTO.parse(annot);

        String path = watchEventDTO.getPath();


        if (path == null || path.equals(".") || path.equals("/"))
            path = "";
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 2);



        Enumeration<URL> en = classLoader.getResources(path);
        while (en.hasMoreElements()) {
            try {
                URI uri = en.nextElement().toURI();


                try {
                    new File(uri);
                } catch (Exception e) {
                    LOGGER.trace("Skipping uri {}, not a local file.", uri);
                    continue;
                }


                LOGGER.debug("Registering resource listener on classpath URI {}", uri);
                registerResourceListener(pluginAnnotation, watchEventDTO, classLoader, uri);
            } catch (URISyntaxException e) {
                LOGGER.error("Unable convert root resource path URL to URI", e);
            }
        }


        if (!watchEventDTO.isClassFileEvent()) {
            for (URL url : pluginManager.getPluginConfiguration(classLoader).getWatchResources()) {
                try {
                    Path watchResourcePath = Paths.get(url.toURI());
                    Path pathInWatchResource = watchResourcePath.resolve(path);
                    if (pathInWatchResource.toFile().exists()) {
                        LOGGER.debug("Registering resource listener on watchResources URI {}", pathInWatchResource.toUri());
                        registerResourceListener(pluginAnnotation, watchEventDTO, classLoader, pathInWatchResource.toUri());
                    }
                } catch (URISyntaxException e) {
                    LOGGER.error("Unable convert watch resource path URL {} to URI", e, url);
                }
            }
        }
    }


    private void registerResourceListener(final PluginAnnotation<T> pluginAnnotation, final WatchEventDTO watchEventDTO,
                                          final ClassLoader classLoader, URI uri) throws IOException {
        pluginManager.getWatcher().addEventListener(classLoader, uri, new WatchEventListener() {
            @Override
            public void onEvent(WatchFileEvent event) {
                WatchEventCommand<T> command = WatchEventCommand.createCmdForEvent(pluginAnnotation, event, classLoader);
                if (command != null) {
                    pluginManager.getScheduler().scheduleCommand(command, watchEventDTO.getTimeout());
                    LOGGER.trace("Resource changed {}", event);
                }
            }
        });
    }


}
