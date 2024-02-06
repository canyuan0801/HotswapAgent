
package org.hotswap.agent.plugin.spring.testBeans;

public class BeanServiceNoAutowireImpl implements BeanService {
    BeanRepository beanRepository;

    @Override
    public String hello() {
        return beanRepository.hello() + " Service";
    }

    public void setBeanRepository(BeanRepository beanRepository) {
        this.beanRepository = beanRepository;
    }

    @Override
    public String isInjectFieldInjected() {
        return "no";
    }
}
