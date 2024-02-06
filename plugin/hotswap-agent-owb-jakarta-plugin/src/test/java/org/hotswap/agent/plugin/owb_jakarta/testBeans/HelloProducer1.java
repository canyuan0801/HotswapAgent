
package org.hotswap.agent.plugin.owb_jakarta.testBeans;

import jakarta.enterprise.context.Dependent;


@Dependent
public class HelloProducer1 {
    public String hello() {
        return "HelloProducer1.hello()";
    }
}
