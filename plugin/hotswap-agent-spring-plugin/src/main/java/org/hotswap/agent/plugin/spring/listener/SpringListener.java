
package org.hotswap.agent.plugin.spring.listener;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.EventListener;


public interface SpringListener<E extends SpringEvent<?>> extends EventListener {

    DefaultListableBeanFactory beanFactory();


    void onEvent(E event);

    default boolean shouldSkip(E event) {
        return !isParentOrSelf(beanFactory(), event.getBeanFactory());
    }

    static boolean isParentOrSelf(ConfigurableListableBeanFactory beanFactory,
        ConfigurableListableBeanFactory sourceBeanFactory) {
        if (beanFactory == null) {
            return false;
        }
        if (sourceBeanFactory == beanFactory) {
            return true;
        }
        if (sourceBeanFactory.getParentBeanFactory() == null) {
            return false;
        }
        if (sourceBeanFactory.getParentBeanFactory() instanceof ConfigurableListableBeanFactory) {
            return isParentOrSelf(beanFactory, (ConfigurableListableBeanFactory)
                sourceBeanFactory.getParentBeanFactory());
        }
        return false;
    }

    default int priority() {
        return 10000;
    }

}
