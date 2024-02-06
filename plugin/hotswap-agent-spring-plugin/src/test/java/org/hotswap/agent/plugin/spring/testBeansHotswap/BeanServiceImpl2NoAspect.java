
package org.hotswap.agent.plugin.spring.testBeansHotswap;

import org.hotswap.agent.plugin.spring.testBeans.BeanChangedRepository;
import org.hotswap.agent.plugin.spring.testBeans.iabpp.BeanServiceNoAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class BeanServiceImpl2NoAspect implements BeanServiceNoAspect {
    String name = "Service2";
    @Autowired
    BeanChangedRepository beanChangedRepository;

    @Override
    public String hello() {
        return beanChangedRepository.hello() + " " + name;
    }

}
