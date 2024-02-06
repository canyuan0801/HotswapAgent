
package org.hotswap.agent.plugin.spring.boot.env.v2;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.spring.boot.env.BasePropertiesPropertySourceLoader;
import org.hotswap.agent.plugin.spring.boot.env.ListPropertySourceReloader;
import org.hotswap.agent.util.ReflectionHelper;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;


public class PropertiesPropertySourceLoader extends BasePropertiesPropertySourceLoader<List<Map<String, ?>>> {

    private static AgentLogger LOGGER = AgentLogger.getLogger(PropertiesPropertySourceLoader.class);

    private org.springframework.boot.env.PropertiesPropertySourceLoader propertiesPropertySourceLoader;
    private Resource resource;


    public PropertiesPropertySourceLoader(
        org.springframework.boot.env.PropertiesPropertySourceLoader propertiesPropertySourceLoader,
                                               String name, Resource resource) {
        super(new ListPropertySourceReloader(name, resource));
        this.propertiesPropertySourceLoader = propertiesPropertySourceLoader;
        this.resource = resource;
    }

    
    protected List<Map<String, ?>> doLoad() {
        return (List<Map<String, ?>>) ReflectionHelper.invoke(propertiesPropertySourceLoader, org.springframework.boot.env.PropertiesPropertySourceLoader.class,
                "loadProperties", new Class[]{Resource.class}, resource);
    }
}
