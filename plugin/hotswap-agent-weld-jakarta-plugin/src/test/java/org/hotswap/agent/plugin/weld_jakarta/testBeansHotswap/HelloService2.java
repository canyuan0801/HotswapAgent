
package org.hotswap.agent.plugin.weld_jakarta.testBeansHotswap;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;


@Singleton
public class HelloService2 {
    String name = "HelloService2.hello";

    @Inject
    HelloProducer2 helloProducer;

    public String hello() {
        return name + ":" + helloProducer.hello();
    }
}
