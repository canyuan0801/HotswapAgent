
package org.hotswap.agent.plugin.spring.transformers.api;

import org.hotswap.agent.plugin.spring.api.PropertySourceReloader;

public interface ReloadablePropertySource {

    void setReload(PropertySourceReloader r);

    void reload();
}
