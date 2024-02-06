
package org.hotswap.agent.plugin.owb.testBeans;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;


@Dependent
public class DependentHello1 {
    @Inject
    HelloServiceImpl1 helloService;

    public String hello() {
        return "DependentHello1.hello():" + helloService.hello();
    }
}
