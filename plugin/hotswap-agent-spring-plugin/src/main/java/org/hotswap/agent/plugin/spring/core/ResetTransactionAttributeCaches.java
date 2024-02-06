package org.hotswap.agent.plugin.spring.core;


import org.hotswap.agent.logging.AgentLogger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.transaction.interceptor.AbstractFallbackTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;

import java.lang.reflect.Field;
import java.util.Map;

public class ResetTransactionAttributeCaches {
    private static final AgentLogger LOGGER = AgentLogger.getLogger(ResetTransactionAttributeCaches.class);

    private static Map<Object, TransactionAttribute> attributeCache;
    private static boolean tried = false;

    public static void reset(DefaultListableBeanFactory beanFactory) {
        if (!beanFactory.containsBean("transactionAttributeSource")) {
            return;
        }
        try {
            if (attributeCache == null && !tried) {

                tried = true;
                final AbstractFallbackTransactionAttributeSource transactionAttributeSource = beanFactory.getBean("transactionAttributeSource", AbstractFallbackTransactionAttributeSource.class);
                Field attributeCacheField = AbstractFallbackTransactionAttributeSource.class.getDeclaredField("attributeCache");
                attributeCacheField.setAccessible(true);
                attributeCache = (Map<Object, TransactionAttribute>) attributeCacheField.get(transactionAttributeSource);
            }
            if (attributeCache != null) {
                attributeCache.clear();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to reset @Transactional cache", e);
        }
    }
}
