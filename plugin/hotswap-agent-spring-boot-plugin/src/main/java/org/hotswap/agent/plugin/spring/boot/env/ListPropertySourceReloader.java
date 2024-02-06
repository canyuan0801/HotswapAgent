
package org.hotswap.agent.plugin.spring.boot.env;

import org.hotswap.agent.logging.AgentLogger;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class ListPropertySourceReloader<T> implements HotswapSpringPropertiesReloader<List<Map<String, T>>> {

    private static AgentLogger LOGGER = AgentLogger.getLogger(ListPropertySourceReloader.class);

    protected final String name;
    protected final Resource resource;
    protected List<Map<String, T>> hotswapMapList;

    public ListPropertySourceReloader(String name, Resource resource) {
        this.name = name;
        this.resource = resource;
    }

    Map<String, T> createMap(Map map) {
        return new HotswapSpringReloadMap<>(map);
    }

    void updateItemValue(Map hotswapMap, Map newHotswapMap) {
        if (hotswapMap instanceof HotswapSpringReloadMap) {
            ((HotswapSpringReloadMap) hotswapMap).update(newHotswapMap);
        }
    }

    @Override
    public void update(List<Map<String, T>> newValue) {
        if (newValue == null || newValue.isEmpty()) {
            hotswapMapList = (newValue == null ? Collections.emptyList() : newValue);
            return;
        }
        if (hotswapMapList == null) {
            synchronized (this) {
                if (hotswapMapList == null) {
                    hotswapMapList = new ArrayList<>(newValue.size());
                    for (Map<String, ?> map : newValue) {
                        hotswapMapList.add(createMap(map));
                    }
                    return;
                }
            }
        }
        for (int i = 0; i < hotswapMapList.size(); i++) {
            Map<String, ?> hotswapMap = hotswapMapList.get(i);
            Map<String, ?> newHotswapMap = newValue.get(i);
            updateItemValue(hotswapMap, newHotswapMap);
        }
    }

    @Override
    public List<Map<String, T>> get() {
        return hotswapMapList;
    }
}
