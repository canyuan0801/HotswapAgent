
package org.hotswap.agent.config;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;


public class PluginConfigurationTest {

    @Test
    public void testGetWatchResources() throws Exception {
        PluginConfiguration pluginConfiguration = new PluginConfiguration(getClass().getClassLoader());
        File tempFile = File.createTempFile("test", "test");


        pluginConfiguration.properties.setProperty("watchResources", tempFile.toURI().toURL().toString());
        assertEquals(tempFile.toURI().toURL(), pluginConfiguration.getWatchResources()[0]);


        pluginConfiguration.properties.setProperty("watchResources", tempFile.getAbsolutePath());





        File canonicalFile = tempFile.getCanonicalFile();
        assertEquals(canonicalFile.toURI().toURL(), pluginConfiguration.getWatchResources()[0]);
    }
}
