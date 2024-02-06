
package org.hotswap.agent.plugin.spring.listener;

import java.util.HashSet;
import java.util.Set;

import org.hotswap.agent.logging.AgentLogger;

public class SpringEventSource {

    private final static AgentLogger LOGGER = AgentLogger.getLogger(SpringEventSource.class);
    public static final SpringEventSource INSTANCE = new SpringEventSource();

    private SpringEventSource() {
    }

    private final Set<SpringListener<SpringEvent<?>>> listeners = new HashSet<>();

    public void addListener(SpringListener<SpringEvent<?>> listener) {
        if (listener == null) {
            return;
        }
        synchronized (listeners) {
            if (listeners.contains(listener)) {
                LOGGER.debug("SpringListener already registered, {}", listener);
                return;
            }
            listeners.add(listener);
        }
    }

    public void fireEvent(SpringEvent<?> event) {
        for (SpringListener<SpringEvent<?>> listener : listeners) {
            if (listener.shouldSkip(event)) {
                continue;
            }
            try {
                listener.onEvent(event);
            } catch (Throwable e) {
                LOGGER.warning("SpringListener onEvent error", e);
            }
        }
    }
}
