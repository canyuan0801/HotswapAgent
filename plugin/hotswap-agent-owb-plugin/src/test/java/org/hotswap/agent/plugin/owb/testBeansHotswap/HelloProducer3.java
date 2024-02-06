
package org.hotswap.agent.plugin.owb.testBeansHotswap;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;


@Dependent
public class HelloProducer3 {

    @Inject
    HelloProducer2 changedHello;

    public String hello() {
        return "HelloProducer3.hello():" + changedHello.hello();
    }

    public String helloNewMethod() {
        return "HelloProducer3.helloNewMethod()";
    }
}
