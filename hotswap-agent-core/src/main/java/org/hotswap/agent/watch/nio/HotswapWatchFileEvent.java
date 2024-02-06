
package org.hotswap.agent.watch.nio;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.net.URI;
import java.nio.file.Files;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

import org.hotswap.agent.annotation.FileEvent;
import org.hotswap.agent.watch.WatchFileEvent;


public class HotswapWatchFileEvent implements WatchFileEvent {

    private final WatchEvent<?> event;
    private final Path path;

    public HotswapWatchFileEvent(WatchEvent<?> event, Path path) {
        this.event = event;
        this.path = path;
    }

    @Override
    public FileEvent getEventType() {
        return toAgentEvent(event.kind());
    }

    @Override
    public URI getURI() {
        return path.toUri();
    }

    @Override
    public boolean isFile() {

        return !isDirectory();
    }

    @Override
    public boolean isDirectory() {
        return Files.isDirectory(path);
    }

    @Override
    public String toString() {
        return "WatchFileEvent on path " + path + " for event " + event.kind();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HotswapWatchFileEvent that = (HotswapWatchFileEvent) o;

        if (!event.equals(that.event)) {
            return false;
        }
        if (!path.equals(that.path)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = event.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }




    static FileEvent toAgentEvent(WatchEvent.Kind<?> kind) {
        if (kind == ENTRY_CREATE) {
            return FileEvent.CREATE;
        } else if (kind == ENTRY_MODIFY) {
            return FileEvent.MODIFY;
        } else if (kind == ENTRY_DELETE) {
            return FileEvent.DELETE;
        } else {
            throw new IllegalArgumentException("Unknown event type " + kind.name());
        }
    }
}