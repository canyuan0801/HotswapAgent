
package org.hotswap.agent.plugin.spring.testBeans;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class BeanServiceImpl implements BeanService {
    @Autowired
    BeanRepository beanRepository;

    @Inject
    BeanChangedRepository beanRepositoryWithInject;

    @Override
    public String hello() {
        if (beanRepository == null) {
            System.out.println("xxxx====: " + this);
        }
        return beanRepository.hello() + " Service";
    }

    @Override
    public String isInjectFieldInjected() {
        return "injected:" + (beanRepositoryWithInject != null);
    }
}
