
package org.hotswap.agent.annotation.handler;

import org.hotswap.agent.annotation.FileEvent;
import org.hotswap.agent.annotation.OnClassFileEvent;
import org.hotswap.agent.annotation.OnResourceFileEvent;
import org.hotswap.agent.watch.WatchFileEvent;

import java.lang.annotation.Annotation;


public class WatchEventDTO {
    private final boolean classFileEvent;
    private final int timeout;
    private final FileEvent[] events;
    private final String classNameRegexp;
    private final String filter;
    private final String path;
    private final boolean onlyRegularFiles;


    public static <T extends Annotation> WatchEventDTO parse(T annotation) {
        if (annotation instanceof OnClassFileEvent)
            return new WatchEventDTO((OnClassFileEvent)annotation);
        else if (annotation instanceof OnResourceFileEvent)
            return new WatchEventDTO((OnResourceFileEvent)annotation);
        else
            throw new IllegalArgumentException("Invalid annotation type " + annotation);
    }

    public WatchEventDTO(OnClassFileEvent annotation) {
        classFileEvent = true;
        timeout = annotation.timeout();
        classNameRegexp = annotation.classNameRegexp();
        events = annotation.events();
        onlyRegularFiles = true;
        filter = null;
        path = null;
    }

    public WatchEventDTO(OnResourceFileEvent annotation) {
        classFileEvent = false;
        timeout = annotation.timeout();
        filter = annotation.filter();
        path = annotation.path();
        events = annotation.events();
        onlyRegularFiles = annotation.onlyRegularFiles();
        classNameRegexp = null;
    }

    public boolean isClassFileEvent() {
        return classFileEvent;
    }

    public int getTimeout() {
        return timeout;
    }

    public FileEvent[] getEvents() {
        return events;
    }

    public String getClassNameRegexp() {
        return classNameRegexp;
    }

    public String getFilter() {
        return filter;
    }

    public String getPath() {
        return path;
    }

    public boolean isOnlyRegularFiles() {
        return onlyRegularFiles;
    }


    public boolean accept(WatchFileEvent event) {


        if (!event.isFile()) {
            return false;
        }




        if (isClassFileEvent() &&  (!event.getURI().toString().endsWith(".class") || event.getURI().toString().endsWith("_jsp.class"))) {
            return false;
        }

        return true;
    }
}
