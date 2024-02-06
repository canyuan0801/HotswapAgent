
package org.hotswap.agent.config;


public interface PluginInitializedListener {
    void pluginIntialized(Object plugin, ClassLoader classLoader);
}
