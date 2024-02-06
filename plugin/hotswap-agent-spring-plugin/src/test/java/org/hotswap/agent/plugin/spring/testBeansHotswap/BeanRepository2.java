
package org.hotswap.agent.plugin.spring.testBeansHotswap;

import org.hotswap.agent.plugin.spring.testBeans.BeanChangedRepository;
import org.hotswap.agent.plugin.spring.testBeans.BeanOtherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;


@Repository
public class BeanRepository2 {
    @Autowired
    BeanOtherRepository beanOtherRepository;

    public String hello() {
        return beanOtherRepository.hello() + "2";
    }

    public String helloNewMethod() {
        return "Repository new method";
    }
}
