
package org.hotswap.agent.watch.nio;


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;


public class TreeWatcherNIO extends AbstractNIO2Watcher {


    private final static WatchEvent.Modifier HIGH;
    private final static WatchEvent.Modifier FILE_TREE;
    private final static WatchEvent.Modifier[] MODIFIERS;

    static {

        HIGH =  getWatchEventModifier("com.sun.nio.file.SensitivityWatchEventModifier","HIGH");

        FILE_TREE = getWatchEventModifier("com.sun.nio.file.ExtendedWatchEventModifier", "FILE_TREE");

        if(FILE_TREE != null) {
            MODIFIERS =  new WatchEvent.Modifier[] { FILE_TREE, HIGH };
        } else {
            MODIFIERS =  new WatchEvent.Modifier[] { HIGH };
        }
    }

    public TreeWatcherNIO() throws IOException {
        super();
    }


    private void register(Path dir) throws IOException {

        for(Path p: keys.values()) {

            if(dir.startsWith(p)) {
                LOGGER.debug("Path {} watched via {}", dir, p);
                return;
            }
        }

        if (FILE_TREE == null) {
            LOGGER.debug("WATCHING:ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY - high} {}", dir);
        } else {
            LOGGER.debug("WATCHING: ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY - fileTree,high {}", dir);
        }

        final WatchKey key = dir.register(watcher, KINDS,  MODIFIERS);

        keys.put(key, dir);
    }


    @Override
    protected void registerAll(Path dir) throws IOException {
        LOGGER.info("Registering directory {} ", dir);
        register(dir);
    }
}