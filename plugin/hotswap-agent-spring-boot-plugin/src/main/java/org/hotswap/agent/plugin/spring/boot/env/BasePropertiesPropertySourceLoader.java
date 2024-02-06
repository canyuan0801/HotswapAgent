
package org.hotswap.agent.plugin.spring.boot.env;

import org.hotswap.agent.plugin.spring.api.PropertySourceReloader;


public abstract class BasePropertiesPropertySourceLoader<T> implements PropertySourceReloader<T> {

    protected HotswapSpringPropertiesReloader<T> properties;

    public BasePropertiesPropertySourceLoader(HotswapSpringPropertiesReloader<T> reloader) {
        this.properties = reloader;
    }


    public final void reload() {
        properties.update(this::doLoad);
    }


    public final T load() {
        this.reload();
        return properties.get();
    }


    protected abstract T doLoad();
}
