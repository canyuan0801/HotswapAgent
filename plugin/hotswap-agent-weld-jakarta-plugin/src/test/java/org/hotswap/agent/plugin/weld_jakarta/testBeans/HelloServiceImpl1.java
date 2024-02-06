
package org.hotswap.agent.plugin.weld_jakarta.testBeans;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;



@Singleton
public class HelloServiceImpl1 extends HelloService {
    @Inject
    HelloProducer1 helloProducer;

    public String hello() {
        return "HelloServiceImpl1.hello():" + helloProducer.hello();
    }
}
