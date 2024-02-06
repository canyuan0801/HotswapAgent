
package org.hotswap.agent.plugin.spring.core;

import org.hotswap.agent.logging.AgentLogger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;

import java.lang.reflect.Field;
import java.util.Set;

public class ResetBeanFactoryPostProcessorCaches {
    private static final AgentLogger LOGGER = AgentLogger.getLogger(ResetBeanFactoryPostProcessorCaches.class);

    public static void reset(DefaultListableBeanFactory beanFactory) {
        resetConfigurationClassPostProcessorCache(beanFactory);
    }

    private static void resetConfigurationClassPostProcessorCache(DefaultListableBeanFactory beanFactory) {
        LOGGER.trace("Resetting ConfigurationClassPostProcessor caches");
        int factoryId = System.identityHashCode(beanFactory);
        try {
            ConfigurationClassPostProcessor ccpp = beanFactory.getBean(ConfigurationClassPostProcessor.class);
            clearSetFieldOfConfigurationClassPostProcessor(ccpp, "factoriesPostProcessed", factoryId);
            clearSetFieldOfConfigurationClassPostProcessor(ccpp, "registriesPostProcessed", factoryId);
        } catch (NoSuchBeanDefinitionException e) {
            LOGGER.trace("ConfigurationClassPostProcessor bean doesn't present");
        }
    }

    private static void clearSetFieldOfConfigurationClassPostProcessor(ConfigurationClassPostProcessor ccpp,
                                                                       String fieldName, int factoryId) {
        try {
            Field field = ConfigurationClassPostProcessor.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Set<Integer> set = (Set<Integer>) field.get(ccpp);
            set.remove(factoryId);
        } catch (Exception e) {
            LOGGER.debug("Error while resetting ConfigurationClassPostProcessor caches", e);
        }
    }
}
