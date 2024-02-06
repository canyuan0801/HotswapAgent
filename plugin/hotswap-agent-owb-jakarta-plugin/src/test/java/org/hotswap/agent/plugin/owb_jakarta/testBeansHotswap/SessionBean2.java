
package org.hotswap.agent.plugin.owb_jakarta.testBeansHotswap;

import java.io.Serializable;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;

import org.hotswap.agent.plugin.owb_jakarta.testBeans.ProxyHello1;

@SessionScoped
public class SessionBean2 implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ProxyHello2 proxyHello;

    @Inject
    private ProxyHello1 proxyHello2;

    public String hello() {
        return "SessionBean2.hello()" + ":" + proxyHello.hello() + ":" + proxyHello2.hello();
    }
}
