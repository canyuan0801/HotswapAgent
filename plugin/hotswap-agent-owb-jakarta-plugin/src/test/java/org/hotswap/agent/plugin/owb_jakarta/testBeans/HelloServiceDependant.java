
package org.hotswap.agent.plugin.owb_jakarta.testBeans;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;


@Dependent
public class HelloServiceDependant {

    @Inject
    HelloProducer1 helloProducer;

    public String hello() {
        return "HelloServiceDependant.hello():" + helloProducer.hello();
    }
}
