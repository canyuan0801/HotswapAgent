

package org.hotswap.agent.util.spring.io.loader;

import org.hotswap.agent.util.spring.io.resource.ClassPathResource;
import org.hotswap.agent.util.spring.io.resource.ContextResource;
import org.hotswap.agent.util.spring.io.resource.Resource;


import org.hotswap.agent.util.spring.util.Assert;
import org.hotswap.agent.util.spring.util.StringUtils;


public class ClassRelativeResourceLoader extends DefaultResourceLoader {

    private final Class<?> clazz;


    public ClassRelativeResourceLoader(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        this.clazz = clazz;
        setClassLoader(clazz.getClassLoader());
    }

    @Override
    protected Resource getResourceByPath(String path) {
        return new ClassRelativeContextResource(path, this.clazz);
    }


    private static class ClassRelativeContextResource extends ClassPathResource implements ContextResource {

        private final Class<?> clazz;

        public ClassRelativeContextResource(String path, Class<?> clazz) {
            super(path, clazz);
            this.clazz = clazz;
        }

        @Override
        public String getPathWithinContext() {
            return getPath();
        }

        @Override
        public Resource createRelative(String relativePath) {
            String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
            return new ClassRelativeContextResource(pathToUse, this.clazz);
        }
    }

}