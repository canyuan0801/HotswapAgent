
package org.hotswap.agent.plugin.weld_jakarta.testBeansHotswap;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.hotswap.agent.plugin.weld_jakarta.testBeans.HelloService;


@Singleton
@Alternative
public class HelloServiceImpl2 extends HelloService {
    String name;

    @Inject
    HelloProducer2 helloProducer;

    public String hello() {
        return name + ":" + helloProducer.hello();
    }

    public String helloNewMethod() {
        return "HelloServiceImpl2.helloNewMethod()";
    }

    public void initName() {
        this.name = "HelloServiceImpl2.hello(initialized)";
    }
}
