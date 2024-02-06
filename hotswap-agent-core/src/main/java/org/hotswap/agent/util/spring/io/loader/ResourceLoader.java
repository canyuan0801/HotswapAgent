

package org.hotswap.agent.util.spring.io.loader;

import org.hotswap.agent.util.spring.io.resource.Resource;
import org.hotswap.agent.util.spring.util.ResourceUtils;



public interface ResourceLoader {


    String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;


    Resource getResource(String location);


    ClassLoader getClassLoader();

}