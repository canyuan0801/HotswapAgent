

package org.hotswap.agent.util.spring.io.loader;

import java.net.MalformedURLException;
import java.net.URL;

import org.hotswap.agent.util.spring.io.resource.ClassPathResource;
import org.hotswap.agent.util.spring.io.resource.ContextResource;
import org.hotswap.agent.util.spring.io.resource.Resource;
import org.hotswap.agent.util.spring.io.resource.UrlResource;
import org.hotswap.agent.util.spring.util.Assert;
import org.hotswap.agent.util.spring.util.ClassUtils;
import org.hotswap.agent.util.spring.util.StringUtils;


public class DefaultResourceLoader implements ResourceLoader {

    private ClassLoader classLoader;


    public DefaultResourceLoader() {
        this.classLoader = ClassUtils.getDefaultClassLoader();
    }


    public DefaultResourceLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }


    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }


    @Override
    public ClassLoader getClassLoader() {
        return (this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader());
    }

    @Override
    public Resource getResource(String location) {
        Assert.notNull(location, "Location must not be null");
        if (location.startsWith("/")) {
            return getResourceByPath(location);
        } else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
            return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
        } else {
            try {

                URL url = new URL(location);
                return new UrlResource(url);
            } catch (MalformedURLException ex) {

                return getResourceByPath(location);
            }
        }
    }


    protected Resource getResourceByPath(String path) {
        return new ClassPathContextResource(path, getClassLoader());
    }


    protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

        public ClassPathContextResource(String path, ClassLoader classLoader) {
            super(path, classLoader);
        }

        @Override
        public String getPathWithinContext() {
            return getPath();
        }

        @Override
        public Resource createRelative(String relativePath) {
            String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
            return new ClassPathContextResource(pathToUse, getClassLoader());
        }
    }

}