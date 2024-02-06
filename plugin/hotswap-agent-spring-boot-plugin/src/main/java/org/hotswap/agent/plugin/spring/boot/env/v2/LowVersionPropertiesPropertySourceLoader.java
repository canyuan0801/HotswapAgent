
package org.hotswap.agent.plugin.spring.boot.env.v2;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.spring.boot.env.BasePropertiesPropertySourceLoader;
import org.hotswap.agent.plugin.spring.boot.env.HotswapSpringReloadMap;
import org.hotswap.agent.util.ReflectionHelper;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.io.Resource;

import java.util.Map;

public class LowVersionPropertiesPropertySourceLoader extends BasePropertiesPropertySourceLoader<Map<String, ?>> {

    private static AgentLogger LOGGER = AgentLogger.getLogger(LowVersionPropertiesPropertySourceLoader.class);

    private PropertiesPropertySourceLoader propertiesPropertySourceLoader;
    private HotswapSpringReloadMap hotswapSpringReloadMap;
    private Resource resource;


    public LowVersionPropertiesPropertySourceLoader(PropertiesPropertySourceLoader propertiesPropertySourceLoader,
                                                         String name, Resource resource) {
        super(new HotswapSpringReloadMap());
        this.propertiesPropertySourceLoader = propertiesPropertySourceLoader;
        this.resource = resource;
    }


    protected Map<String, ?> doLoad() {
        return (Map<String, ?>) ReflectionHelper.invoke(propertiesPropertySourceLoader, PropertiesPropertySourceLoader.class,
                "loadProperties", new Class[]{Resource.class}, resource);
    }
}
