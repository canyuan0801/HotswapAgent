
package org.hotswap.agent.plugin.spring.listener;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.EventObject;


public abstract class SpringEvent<T> extends EventObject {

    private ConfigurableListableBeanFactory beanFactory;


    public SpringEvent(T source, ConfigurableListableBeanFactory beanFactory) {
        super(source);
        this.beanFactory = beanFactory;
    }

    public T getSource() {
        return (T) super.getSource();
    }

    public ConfigurableListableBeanFactory getBeanFactory() {
        return beanFactory;
    }
}
