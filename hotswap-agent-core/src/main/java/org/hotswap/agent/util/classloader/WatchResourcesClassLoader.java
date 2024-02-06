
package org.hotswap.agent.util.classloader;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.watch.WatchFileEvent;
import org.hotswap.agent.watch.WatchEventListener;
import org.hotswap.agent.watch.Watcher;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;


public class WatchResourcesClassLoader extends URLClassLoader {
    private static AgentLogger LOGGER = AgentLogger.getLogger(WatchResourcesClassLoader.class);


    Set<URL> changedUrls = new HashSet<>();


    boolean searchParent = true;

    public void setSearchParent(boolean searchParent) {
        this.searchParent = searchParent;
    }


    ClassLoader watchResourcesClassLoader;

    public WatchResourcesClassLoader() {
        this(false);
    }

    public WatchResourcesClassLoader(boolean searchParent) {
        super(new URL[]{}, searchParent ? WatchResourcesClassLoader.class.getClassLoader() : null);
        this.searchParent = searchParent;
    }

    public WatchResourcesClassLoader(ClassLoader classLoader) {
        super(new URL[] {}, classLoader);
        this.searchParent = false;
    }


    public void initExtraPath(URL[] extraPath) {
        for (URL url : extraPath)
            addURL(url);
    }


    public void initWatchResources(URL[] watchResources, Watcher watcher) {

        this.watchResourcesClassLoader = new UrlOnlyClassLoader(watchResources);


        for (URL resource : watchResources) {
            try {
                URI uri = resource.toURI();
                LOGGER.debug("Watching directory '{}' for changes.", uri);
                watcher.addEventListener(this, uri, new WatchEventListener() {
                    @Override
                    public void onEvent(WatchFileEvent event) {
                        try {
                            if (event.isFile() || event.isDirectory()) {
                                changedUrls.add(event.getURI().toURL());
                                LOGGER.trace("File '{}' changed and will be returned instead of original classloader equivalent.", event.getURI().toURL());
                            }
                        } catch (MalformedURLException e) {
                            LOGGER.error("Unexpected - cannot convert URI {} to URL.", e, event.getURI());
                        }
                    }
                });
            } catch (URISyntaxException e) {
                LOGGER.warning("Unable to convert watchResources URL '{}' to URI. URL is skipped.", e, resource);
            }
        }
    }


    public boolean isResourceChanged(URL url) {
        return changedUrls.contains(url);
    }


    @Override
    public URL getResource(String name) {
        if (watchResourcesClassLoader != null) {
            URL resource = watchResourcesClassLoader.getResource(name);
            if (resource != null && isResourceChanged(resource)) {
                LOGGER.trace("watchResources - using changed resource {}", name);
                return resource;
            }
        }


        URL resource = findResource(name);
        if (resource != null)
            return resource;


        if (searchParent)
            return super.getResource(name);
        else
            return null;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        try {
            return url != null ? url.openStream() : null;
        } catch (IOException e) {
        }
        return null;
    }


    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (watchResourcesClassLoader != null) {
            URL resource = watchResourcesClassLoader.getResource(name);
            if (resource != null && isResourceChanged(resource)) {
                LOGGER.trace("watchResources - using changed resource {}", name);
                Vector<URL> res = new Vector<>();
                res.add(resource);
                return res.elements();
            }
        }


        if (findResources(name).hasMoreElements())
            return findResources(name);

        return super.getResources(name);
    }


    public String getClasspath() {
        ClassLoader parent = getParent();

        if (parent == null)
            return null;

        try {
            Method m = parent.getClass().getMethod("getClasspath", new Class[] {});
            if( m==null ) return null;
            Object o = m.invoke( parent, new Object[] {} );
            if( o instanceof String )
                return (String)o;
            return null;
        } catch( Exception ex ) {
            LOGGER.debug("getClasspath not supported on parent classloader.");
        }
        return null;

    }


    public static class UrlOnlyClassLoader extends URLClassLoader {
        public UrlOnlyClassLoader(URL[] urls) {
            super(urls);
        }


        @Override
        public URL getResource(String name) {
            return findResource(name);
        }
    };
}
