
package org.hotswap.agent.plugin.weld_jakarta.testBeansHotswap;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.hotswap.agent.plugin.weld_jakarta.testBeans.HelloProducer1;


@Dependent
public class DependentHello2 {
    @Inject
    HelloProducer1 helloProducer;

    public String hello() {
        return "DependentHello2.hello():" + helloProducer.hello();
    }
}
