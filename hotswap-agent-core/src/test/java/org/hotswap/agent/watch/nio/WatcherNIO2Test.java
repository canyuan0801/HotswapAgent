
package org.hotswap.agent.watch.nio;

import org.hotswap.agent.annotation.FileEvent;
import org.hotswap.agent.watch.WatchFileEvent;
import org.hotswap.agent.watch.WatchEventListener;
import org.hotswap.agent.watch.Watcher;
import org.hotswap.agent.watch.WatcherFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;


public class WatcherNIO2Test {

    Watcher watcher;
    Path temp;

    @Before
    public void setup() throws IOException {
        watcher = new WatcherFactory().getWatcher();
        temp = Files.createTempDirectory("watcherNIO2Test");

        watcher.run();
    }

    @After
    public void tearDown() {
        watcher.run();
    }

    @Test
    public void createFile() throws IOException {
        final ResultHolder resultHolder = new ResultHolder();
        watcher.addEventListener(null, temp.toUri(), new WatchEventListener() {
            @Override
            public void onEvent(WatchFileEvent event) {
                assertEquals("New file event type", FileEvent.CREATE, event.getEventType());
                assertTrue("File name", event.getURI().toString().endsWith("test.class"));
                resultHolder.result = true;
            }
        });

        File testFile = new File(temp.toFile(), "test.class");
        testFile.createNewFile();

        assertTrue("Event listener called", waitForResult(resultHolder));
    }



    public void testTargetClasses() throws Exception {
        URI uri = new URI("file:/" + temp);
        final ResultHolder resultHolder = new ResultHolder();
        watcher.addEventListener(null, uri, new WatchEventListener() {
            @Override
            public void onEvent(WatchFileEvent event) {
                assertTrue("File name", event.getURI().toString().endsWith("test.class"));
                resultHolder.result = true;
            }
        });

        File testFile = new File(uri.toURL().getFile(), "test.class");
        testFile.createNewFile();

        assertTrue("Event listener not called", waitForResult(resultHolder));

        testFile.delete();
    }




    private boolean waitForResult(ResultHolder resultHolder) {
        for (int i = 0; i < 1000; i++) {
            if (resultHolder.result)
                return true;


            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        return false;
    }

    private static class ResultHolder {
        boolean result = false;
    }
}
