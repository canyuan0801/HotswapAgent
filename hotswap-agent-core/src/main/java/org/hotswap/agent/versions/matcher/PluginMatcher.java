
package org.hotswap.agent.versions.matcher;

import org.hotswap.agent.annotation.Versions;


public class PluginMatcher extends AbstractMatcher {


    public PluginMatcher(Class<?> pluginClass) {
        super(pluginClass.getAnnotation(Versions.class));
    }

}
