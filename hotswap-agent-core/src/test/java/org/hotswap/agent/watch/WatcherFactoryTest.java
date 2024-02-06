
package org.hotswap.agent.watch;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;


public class WatcherFactoryTest {
    @Test
    public void testGetWatcher() throws Exception {
        assertNotNull(new WatcherFactory().getWatcher());
    }
}
