
package org.hotswap.agent.annotation.handler;

import java.lang.annotation.Annotation;


public interface PluginHandler<T extends Annotation> {


    boolean initField(PluginAnnotation<T> pluginAnnotation);


    boolean initMethod(PluginAnnotation<T> pluginAnnotation);

}
