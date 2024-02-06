
package org.hotswap.agent.plugin.spring.testBeans.iabpp;

import org.hotswap.agent.plugin.spring.testBeans.BeanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class BeanServiceImplNoAspect implements BeanServiceNoAspect {
    @Autowired
    BeanRepository beanRepository;

    @Override
    public String hello() {
        return beanRepository.hello() + " Service";
    }
}
