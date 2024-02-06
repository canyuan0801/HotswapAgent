
package org.hotswap.agent.plugin.spring.boot.listener;

import java.util.Objects;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.spring.files.PropertiesChangeEvent;
import org.hotswap.agent.plugin.spring.listener.SpringEvent;
import org.hotswap.agent.plugin.spring.listener.SpringEventSource;
import org.hotswap.agent.plugin.spring.listener.SpringListener;
import org.hotswap.agent.plugin.spring.reload.BeanChangeEvent;
import org.hotswap.agent.util.AnnotationHelper;
import org.hotswap.agent.util.spring.util.ClassUtils;
import org.hotswap.agent.util.spring.util.ObjectUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;


public class PropertySourceChangeListener implements SpringListener<SpringEvent<?>> {

    private final static AgentLogger LOGGER = AgentLogger.getLogger(PropertySourceChangeListener.class);

    private final DefaultListableBeanFactory beanFactory;

    public static void register(ConfigurableApplicationContext context) {
        ConfigurableListableBeanFactory configurableListableBeanFactory = context.getBeanFactory();
        if (!(configurableListableBeanFactory instanceof DefaultListableBeanFactory)) {
            LOGGER.debug(
                "beanFactory is not DefaultListableBeanFactory, skip register PropertySourceChangeBootListener, {}",
                ObjectUtils.identityToString(configurableListableBeanFactory));
            return;
        }
        LOGGER.debug("register PropertySourceChangeBootListener, {}",
            ObjectUtils.identityToString(configurableListableBeanFactory));
        PropertySourceChangeListener propertySourceChangeListener = new PropertySourceChangeListener(
            (DefaultListableBeanFactory)configurableListableBeanFactory);

        SpringEventSource.INSTANCE.addListener(propertySourceChangeListener);
    }

    public PropertySourceChangeListener(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public DefaultListableBeanFactory beanFactory() {
        return beanFactory;
    }

    @Override
    public void onEvent(SpringEvent<?> event) {
        if (event instanceof PropertiesChangeEvent) {
            refreshConfigurationProperties(event.getBeanFactory());
        }
    }

    private void refreshConfigurationProperties(ConfigurableListableBeanFactory eventBeanFactory) {
        for (String singleton : beanFactory.getSingletonNames()) {
            Object bean = beanFactory.getSingleton(singleton);
            Class<?> beanClass = ClassUtils.getUserClass(bean.getClass());

            if (AnnotationHelper.hasAnnotation(beanClass, ConfigurationProperties.class.getName())) {
                LOGGER.debug("refresh configuration properties: {}", beanClass);
                String[] beanNames = beanFactory.getBeanNamesForType(beanClass);
                if (beanNames != null && beanNames.length > 0) {
                    SpringEventSource.INSTANCE.fireEvent(new BeanChangeEvent(beanNames, eventBeanFactory));
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (!(o instanceof PropertySourceChangeListener)) {return false;}
        PropertySourceChangeListener that = (PropertySourceChangeListener)o;
        return Objects.equals(beanFactory, that.beanFactory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beanFactory);
    }
}
