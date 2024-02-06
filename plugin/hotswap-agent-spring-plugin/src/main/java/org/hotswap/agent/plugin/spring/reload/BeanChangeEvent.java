
package org.hotswap.agent.plugin.spring.reload;

import org.hotswap.agent.plugin.spring.listener.SpringEvent;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;


public class BeanChangeEvent extends SpringEvent<String[]> {

    public BeanChangeEvent(String[] source, ConfigurableListableBeanFactory beanFactory) {
        super(source, beanFactory);
    }
}
