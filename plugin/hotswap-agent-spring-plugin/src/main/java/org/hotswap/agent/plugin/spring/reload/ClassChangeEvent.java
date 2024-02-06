
package org.hotswap.agent.plugin.spring.reload;

import org.hotswap.agent.plugin.spring.listener.SpringEvent;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class ClassChangeEvent extends SpringEvent<Class> {

    public ClassChangeEvent(Class<?> source, ConfigurableListableBeanFactory beanFactory) {
        super(source, beanFactory);
    }
}
