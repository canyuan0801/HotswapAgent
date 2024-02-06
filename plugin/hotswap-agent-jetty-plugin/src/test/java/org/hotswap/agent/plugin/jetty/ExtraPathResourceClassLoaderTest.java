
package org.hotswap.agent.plugin.jetty;

import org.hotswap.agent.util.classloader.WatchResourcesClassLoader;
import org.hotswap.agent.util.test.WaitHelper;
import org.hotswap.agent.watch.Watcher;
import org.hotswap.agent.watch.nio.WatcherNIO2;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class ExtraPathResourceClassLoaderTest {

    @Test
    public void testGetResource() throws Exception {

        final Path directory = Files.createTempDirectory(ExtraPathResourceClassLoaderTest.class.getName());
        final File tempFile = new File(directory.toFile(), "test");
        tempFile.createNewFile();


        final Watcher watcher = new WatcherNIO2();
        watcher.run();


        final WatchResourcesClassLoader classLoader = new WatchResourcesClassLoader();
        classLoader.initWatchResources(new URL[]{directory.toUri().toURL()}, watcher);

        assertNull("Not returned before modification", classLoader.getResource(tempFile.getName()));


        tempFile.setLastModified(new Date().getTime()+1000);


        WaitHelper.waitForCommand(new WaitHelper.Command() {
            @Override
            public boolean result() throws Exception {
                return (classLoader.getResource(tempFile.getName()) != null);
            }
        }, 5000);



        assertNotNull("Returned after modification", classLoader.getResource(tempFile.getName()));
    }
}
