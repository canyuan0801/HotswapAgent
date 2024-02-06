
package org.hotswap.agent.plugin.spring.boot.env;

import java.util.Properties;

public class HotswapSpringProperties extends Properties implements HotswapSpringPropertiesReloader<Properties> {

    public HotswapSpringProperties() {
        super();
    }

    public HotswapSpringProperties(Properties properties) {
        super();
        this.putAll(properties);
    }


    @Override
    public void update(Properties newValue) {
        if (newValue == null || newValue.isEmpty()) {
            this.clear();
            return;
        }
        this.clear();
        this.putAll(newValue);
    }

    @Override
    public Properties get() {
        return this;
    }
}
