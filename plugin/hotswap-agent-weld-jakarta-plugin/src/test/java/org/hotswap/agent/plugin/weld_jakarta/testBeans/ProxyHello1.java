
package org.hotswap.agent.plugin.weld_jakarta.testBeans;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProxyHello1 {
    public String hello() {
        return "ProxyHello1.hello()";
    }
}
