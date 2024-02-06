
package org.hotswap.agent.watch;

import org.hotswap.agent.annotation.FileEvent;

import java.net.URI;


public interface WatchFileEvent {

    
    public FileEvent getEventType();

    
    public URI getURI();

    
    public boolean isFile();

    
    public boolean isDirectory();
}
