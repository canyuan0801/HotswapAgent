
package org.hotswap.agent.plugin.owb_jakarta.testBeansHotswap;

import jakarta.enterprise.context.Dependent;


@Dependent
public class HelloProducer2 {
    public String hello() {
        return "HelloProducer2.hello()";
    }
}
