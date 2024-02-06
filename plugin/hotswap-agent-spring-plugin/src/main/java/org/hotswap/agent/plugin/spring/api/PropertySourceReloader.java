package org.hotswap.agent.plugin.spring.api;


public interface PropertySourceReloader<T> {

    void reload();

    T load();
}
