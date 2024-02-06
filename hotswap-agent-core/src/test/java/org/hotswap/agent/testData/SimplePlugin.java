
package org.hotswap.agent.testData;

import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.annotation.OnClassLoadEvent;


@Plugin(name = "Hibernate plugin", testedVersions = {"1.0"})
public class SimplePlugin {

    @Init
    public void initPlugin() {
    }

    @Init
    public void initPlugin(PluginManager manager) {
    }


    @OnClassLoadEvent(classNameRegexp = "org.hotswap.example.type")
    public void transform() {

    }

    
    public void callPluginMethod(Boolean val) {
    }
}
