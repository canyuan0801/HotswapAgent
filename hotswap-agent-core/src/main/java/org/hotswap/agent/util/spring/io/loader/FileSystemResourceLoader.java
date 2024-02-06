

package org.hotswap.agent.util.spring.io.loader;

import org.hotswap.agent.util.spring.io.resource.ContextResource;
import org.hotswap.agent.util.spring.io.resource.FileSystemResource;
import org.hotswap.agent.util.spring.io.resource.Resource;


public class FileSystemResourceLoader extends DefaultResourceLoader {


    @Override
    protected Resource getResourceByPath(String path) {
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }
        return new FileSystemContextResource(path);
    }


    private static class FileSystemContextResource extends FileSystemResource implements ContextResource {

        public FileSystemContextResource(String path) {
            super(path);
        }

        @Override
        public String getPathWithinContext() {
            return getPath();
        }
    }

}