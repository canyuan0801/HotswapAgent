
package org.hotswap.agent.plugin.owb_jakarta.testBeansHotswap;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProxyHello2 {

    public String hello() {
        return "ProxyHello2.hello()";
    }

    public String hello2() {
        return "ProxyHello2.hello2()";
    }
}
