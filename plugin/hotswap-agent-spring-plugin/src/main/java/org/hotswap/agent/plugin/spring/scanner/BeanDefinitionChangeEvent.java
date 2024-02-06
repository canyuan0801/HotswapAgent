
package org.hotswap.agent.plugin.spring.scanner;

import org.hotswap.agent.plugin.spring.listener.SpringEvent;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class BeanDefinitionChangeEvent extends SpringEvent<BeanDefinitionHolder> {

    public BeanDefinitionChangeEvent(BeanDefinitionHolder source, ConfigurableListableBeanFactory beanFactory) {
        super(source, beanFactory);
    }
}