
package org.hotswap.agent.plugin.spring.boot.env.v1;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.spring.boot.env.BasePropertiesPropertySourceLoader;
import org.hotswap.agent.plugin.spring.boot.env.HotswapSpringReloadMap;
import org.hotswap.agent.util.ReflectionHelper;
import org.springframework.core.io.Resource;

import java.util.Map;

public class YamlPropertySourceLoader extends BasePropertiesPropertySourceLoader<Map<String, Object>> {

    private static AgentLogger LOGGER = AgentLogger.getLogger(YamlPropertySourceLoader.class);

    final String profile;
    final Resource resource;

    public YamlPropertySourceLoader(String name, Resource resource, String profile) {
        super(new HotswapSpringReloadMap());
        this.profile = profile;
        this.resource = resource;
    }

    protected Map<String, Object> doLoad() {
        try {
            Object target = ReflectionHelper.invokeConstructor("org.springframework.boot.env.YamlPropertySourceLoader$Processor",
                    this.getClass().getClassLoader(), new Class[]{Resource.class, String.class}, resource, profile);
            return (Map<String, Object>) ReflectionHelper.invoke(target, "process");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
