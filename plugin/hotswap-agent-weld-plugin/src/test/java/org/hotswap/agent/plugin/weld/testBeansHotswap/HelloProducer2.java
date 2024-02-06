
package org.hotswap.agent.plugin.weld.testBeansHotswap;

import javax.enterprise.context.Dependent;


@Dependent
public class HelloProducer2 {
    public String hello() {
        return "HelloProducer2.hello()";
    }
}
