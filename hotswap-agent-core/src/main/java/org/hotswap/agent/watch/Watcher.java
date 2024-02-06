
package org.hotswap.agent.watch;

import java.net.URI;
import java.net.URL;


public interface Watcher {

    void addEventListener(ClassLoader classLoader, URI pathPrefix, WatchEventListener listener);


    void addEventListener(ClassLoader classLoader, URL pathPrefix, WatchEventListener listener);


    void closeClassLoader(ClassLoader classLoader);



    void run();


    void stop();
}
