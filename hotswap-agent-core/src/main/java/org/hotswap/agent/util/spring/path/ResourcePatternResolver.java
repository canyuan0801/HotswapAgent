

package org.hotswap.agent.util.spring.path;

import java.io.IOException;

import org.hotswap.agent.util.spring.io.loader.ResourceLoader;
import org.hotswap.agent.util.spring.io.resource.Resource;





public interface ResourcePatternResolver extends ResourceLoader {


    String CLASSPATH_ALL_URL_PREFIX = "classpath*:";


    Resource[] getResources(String locationPattern) throws IOException;

}