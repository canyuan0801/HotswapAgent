
package org.hotswap.agent.plugin.spring.testBeansHotswap;

import javax.inject.Inject;

import org.hotswap.agent.plugin.spring.testBeans.BeanChangedRepository;
import org.hotswap.agent.plugin.spring.testBeans.BeanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class BeanServiceImpl2 implements BeanService {
    String name = "Service2";

    @Autowired
    BeanChangedRepository beanChangedRepository;

    @Inject
    BeanChangedRepository beanChangedRepositoryWithInject;

    @Override
    public String hello() {
        if (beanChangedRepository == null) {
            System.out.println("====xxxx:" + this);
        }
        return beanChangedRepository.hello() + " " + name;
    }

    public String helloNewMethod() {
        return "Hello from helloNewMethod " + name;
    }

    @Override
    public String isInjectFieldInjected() {
        return "injectedChanged:" + (beanChangedRepositoryWithInject != null);
    }
}
