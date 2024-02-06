
package org.hotswap.agent.plugin.weld_jakarta.testBeans;

import jakarta.inject.Inject;

public class ProxyHosting {

    @Inject
    public ProxyHello1 proxy;

    public String hello() {
        return proxy.hello();
    }
}
