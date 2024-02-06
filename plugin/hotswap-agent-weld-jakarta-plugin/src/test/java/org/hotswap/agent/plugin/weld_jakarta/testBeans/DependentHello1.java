
package org.hotswap.agent.plugin.weld_jakarta.testBeans;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;


@Dependent
public class DependentHello1 {
    @Inject
    HelloServiceImpl1 helloService;

    public String hello() {
        return "DependentHello1.hello():" + helloService.hello();
    }
}
