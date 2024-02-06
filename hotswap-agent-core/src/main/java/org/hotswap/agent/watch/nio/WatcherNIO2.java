
package org.hotswap.agent.watch.nio;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;


public class WatcherNIO2 extends AbstractNIO2Watcher {
    private final static WatchEvent.Modifier HIGH;

    static {
        HIGH =  getWatchEventModifier("com.sun.nio.file.SensitivityWatchEventModifier","HIGH");
    }

    public WatcherNIO2() throws IOException {
        super();
    }

    @Override
    protected void registerAll(final Path dir) throws IOException {

        LOGGER.debug("Registering directory  {}", dir);

        Files.walkFileTree(dir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }


    private void register(Path dir) throws IOException {

        final WatchKey key = HIGH == null ? dir.register(watcher, KINDS) : dir.register(watcher, KINDS, HIGH);
        keys.put(key, dir);
    }
}