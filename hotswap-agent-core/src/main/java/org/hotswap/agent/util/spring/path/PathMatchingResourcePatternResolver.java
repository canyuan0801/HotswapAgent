

package org.hotswap.agent.util.spring.path;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.ReflectionHelper;
import org.hotswap.agent.util.spring.io.loader.DefaultResourceLoader;
import org.hotswap.agent.util.spring.io.loader.ResourceLoader;
import org.hotswap.agent.util.spring.io.resource.FileSystemResource;
import org.hotswap.agent.util.spring.io.resource.Resource;
import org.hotswap.agent.util.spring.io.resource.UrlResource;
import org.hotswap.agent.util.spring.io.resource.VfsResource;
import org.hotswap.agent.util.spring.util.Assert;
import org.hotswap.agent.util.spring.util.ClassUtils;
import org.hotswap.agent.util.spring.util.ReflectionUtils;
import org.hotswap.agent.util.spring.util.ResourceUtils;
import org.hotswap.agent.util.spring.util.StringUtils;


public class PathMatchingResourcePatternResolver implements ResourcePatternResolver {
    private static AgentLogger logger = AgentLogger.getLogger(PathMatchingResourcePatternResolver.class);

    private static Method equinoxResolveMethod;

    static {
        try {

            Class<?> fileLocatorClass = ClassUtils.forName("org.eclipse.core.runtime.FileLocator", PathMatchingResourcePatternResolver.class.getClassLoader());
            equinoxResolveMethod = fileLocatorClass.getMethod("resolve", URL.class);
            logger.debug("Found Equinox FileLocator for OSGi bundle URL resolution");
        } catch (Throwable ex) {
            equinoxResolveMethod = null;
        }
    }

    private final ResourceLoader resourceLoader;

    private PathMatcher pathMatcher = new AntPathMatcher();


    public PathMatchingResourcePatternResolver() {
        this.resourceLoader = new DefaultResourceLoader();
    }


    public PathMatchingResourcePatternResolver(ResourceLoader resourceLoader) {
        Assert.notNull(resourceLoader, "ResourceLoader must not be null");
        this.resourceLoader = resourceLoader;
    }


    public PathMatchingResourcePatternResolver(ClassLoader classLoader) {
        this.resourceLoader = new DefaultResourceLoader(classLoader);
    }


    public ResourceLoader getResourceLoader() {
        return this.resourceLoader;
    }

    @Override
    public ClassLoader getClassLoader() {
        return getResourceLoader().getClassLoader();
    }


    public void setPathMatcher(PathMatcher pathMatcher) {
        Assert.notNull(pathMatcher, "PathMatcher must not be null");
        this.pathMatcher = pathMatcher;
    }


    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }

    @Override
    public Resource getResource(String location) {
        return getResourceLoader().getResource(location);
    }

    @Override
    public Resource[] getResources(String locationPattern) throws IOException {
        Assert.notNull(locationPattern, "Location pattern must not be null");
        if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {

            if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {

                return findPathMatchingResources(locationPattern);
            } else {

                return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
            }
        } else {


            int prefixEnd = locationPattern.indexOf(":") + 1;
            if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {

                return findPathMatchingResources(locationPattern);
            } else {

                return new Resource[] { getResourceLoader().getResource(locationPattern) };
            }
        }
    }


    protected Resource[] findAllClassPathResources(String location) throws IOException {
        String path = location;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        Set<Resource> result = doFindAllClassPathResources(path);
        return result.toArray(new Resource[result.size()]);
    }


    protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
        Set<Resource> result = new LinkedHashSet<Resource>(16);
        ClassLoader cl = getClassLoader();
        Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
        while (resourceUrls.hasMoreElements()) {
            URL url = resourceUrls.nextElement();
            result.add(convertClassLoaderURL(url));
        }
        if ("".equals(path)) {




            addAllClassLoaderJarRoots(cl, result);
        }
        return result;
    }


    protected Resource convertClassLoaderURL(URL url) {
        return new UrlResource(url);
    }


    protected void addAllClassLoaderJarRoots(ClassLoader classLoader, Set<Resource> result) {
        if (classLoader instanceof URLClassLoader) {
            try {
                for (URL url : ((URLClassLoader) classLoader).getURLs()) {
                    if (ResourceUtils.isJarFileURL(url)) {
                        try {
                            UrlResource jarResource = new UrlResource(ResourceUtils.JAR_URL_PREFIX + url.toString() + ResourceUtils.JAR_URL_SEPARATOR);
                            if (jarResource.exists()) {
                                result.add(jarResource);
                            }
                        } catch (MalformedURLException ex) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Cannot search for matching files underneath [" + url + "] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cannot introspect jar files since ClassLoader [" + classLoader + "] does not support 'getURLs()': " + ex);
                }
            }
        }
        if (classLoader != null) {
            try {
                addAllClassLoaderJarRoots(classLoader.getParent(), result);
            } catch (Exception ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cannot introspect jar files in parent ClassLoader since [" + classLoader + "] does not support 'getParent()': " + ex);
                }
            }
        }
    }


    protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
        String rootDirPath = determineRootDir(locationPattern);
        String subPattern = locationPattern.substring(rootDirPath.length());
        Resource[] rootDirResources = getResources(rootDirPath);
        Set<Resource> result = new LinkedHashSet<Resource>(16);
        for (Resource rootDirResource : rootDirResources) {
            rootDirResource = resolveRootDirResource(rootDirResource);
            if (rootDirResource.getURL().getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
                result.addAll(VfsResourceMatchingDelegate.findMatchingResources(rootDirResource, subPattern, getPathMatcher()));
            } else if (isJarResource(rootDirResource)) {
                result.addAll(doFindPathMatchingJarResources(rootDirResource, subPattern));
            } else {
                result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Resolved location pattern [" + locationPattern + "] to resources " + result);
        }
        return result.toArray(new Resource[result.size()]);
    }


    protected String determineRootDir(String location) {
        int prefixEnd = location.indexOf(":") + 1;
        int rootDirEnd = location.length();
        while (rootDirEnd > prefixEnd && getPathMatcher().isPattern(location.substring(prefixEnd, rootDirEnd))) {
            rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
        }
        if (rootDirEnd == 0) {
            rootDirEnd = prefixEnd;
        }
        return location.substring(0, rootDirEnd);
    }


    protected Resource resolveRootDirResource(Resource original) throws IOException {
        URL url = original.getURL();
        if (url != null && url.getProtocol().startsWith("bundle")) {
            if (equinoxResolveMethod != null) {
                return new UrlResource((URL) ReflectionUtils.invokeMethod(equinoxResolveMethod, null, url));
            }

            if (url.getProtocol().equals("bundle")) {
                try {

                    Class<?> bundleWiringClass = ClassUtils.forName("org.apache.felix.framework.BundleWiringImpl", null);
                    URL convertedURL = (URL) ReflectionHelper.invoke(null, bundleWiringClass, "convertToLocalUrl", new Class[] { URL.class }, url);
                    return new UrlResource(convertedURL);
                } catch (ClassNotFoundException e) {
                    logger.trace("org.apache.felix.framework.BundleWiringImpl class not found for {}", url);
                } catch (Exception e) {
                    logger.error("Felix BundleWiring.convertToLocalUrl(URL) failed for URL {}", url, e);
                }
            }
        }

        return original;
    }


    protected boolean isJarResource(Resource resource) throws IOException {
        return ResourceUtils.isJarURL(resource.getURL());
    }


    protected Set<Resource> doFindPathMatchingJarResources(Resource rootDirResource, String subPattern) throws IOException {

        URLConnection con = rootDirResource.getURL().openConnection();
        JarFile jarFile;
        String jarFileUrl;
        String rootEntryPath;
        boolean newJarFile = false;

        if (con instanceof JarURLConnection) {

            JarURLConnection jarCon = (JarURLConnection) con;
            ResourceUtils.useCachesIfNecessary(jarCon);
            jarFile = jarCon.getJarFile();
            jarFileUrl = jarCon.getJarFileURL().toExternalForm();
            JarEntry jarEntry = jarCon.getJarEntry();
            rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
        } else {





            String urlFile = rootDirResource.getURL().getFile();
            try {
                int separatorIndex = urlFile.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
                if (separatorIndex != -1) {
                    jarFileUrl = urlFile.substring(0, separatorIndex);
                    rootEntryPath = urlFile.substring(separatorIndex + ResourceUtils.JAR_URL_SEPARATOR.length());
                    jarFile = getJarFile(jarFileUrl);
                } else {
                    jarFile = new JarFile(urlFile);
                    jarFileUrl = urlFile;
                    rootEntryPath = "";
                }
                newJarFile = true;
            } catch (ZipException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping invalid jar classpath entry [" + urlFile + "]");
                }
                return Collections.emptySet();
            }
        }

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Looking for matching resources in jar file [" + jarFileUrl + "]");
            }
            if (!"".equals(rootEntryPath) && !rootEntryPath.endsWith("/")) {




                rootEntryPath = rootEntryPath + "/";
            }
            Set<Resource> result = new LinkedHashSet<Resource>(8);
            for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                String entryPath = entry.getName();
                if (entryPath.startsWith(rootEntryPath)) {
                    String relativePath = entryPath.substring(rootEntryPath.length());
                    if (getPathMatcher().match(subPattern, relativePath)) {
                        result.add(rootDirResource.createRelative(relativePath));
                    }
                }
            }
            return result;
        } finally {


            if (newJarFile) {
                jarFile.close();
            }
        }
    }


    protected JarFile getJarFile(String jarFileUrl) throws IOException {
        if (jarFileUrl.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
            try {
                return new JarFile(ResourceUtils.toURI(jarFileUrl).getSchemeSpecificPart());
            } catch (URISyntaxException ex) {


                return new JarFile(jarFileUrl.substring(ResourceUtils.FILE_URL_PREFIX.length()));
            }
        } else {
            return new JarFile(jarFileUrl);
        }
    }


    protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern) throws IOException {

        File rootDir;
        try {
            rootDir = rootDirResource.getFile().getAbsoluteFile();
        } catch (IOException ex) {
            if (logger.isWarnEnabled()) {
                logger.warning("Cannot search for matching files underneath {} because it does not correspond to a directory in the file system", rootDirResource);
            }
            return Collections.emptySet();
        }
        return doFindMatchingFileSystemResources(rootDir, subPattern);
    }


    protected Set<Resource> doFindMatchingFileSystemResources(File rootDir, String subPattern) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Looking for matching resources in directory tree [" + rootDir.getPath() + "]");
        }
        Set<File> matchingFiles = retrieveMatchingFiles(rootDir, subPattern);
        Set<Resource> result = new LinkedHashSet<Resource>(matchingFiles.size());
        for (File file : matchingFiles) {
            result.add(new FileSystemResource(file));
        }
        return result;
    }


    protected Set<File> retrieveMatchingFiles(File rootDir, String pattern) throws IOException {
        if (!rootDir.exists()) {

            if (logger.isDebugEnabled()) {
                logger.debug("Skipping [" + rootDir.getAbsolutePath() + "] because it does not exist");
            }
            return Collections.emptySet();
        }
        if (!rootDir.isDirectory()) {

            if (logger.isWarnEnabled()) {
                logger.warning("Skipping [" + rootDir.getAbsolutePath() + "] because it does not denote a directory");
            }
            return Collections.emptySet();
        }
        if (!rootDir.canRead()) {
            if (logger.isWarnEnabled()) {
                logger.warning("Cannot search for matching files underneath directory [" + rootDir.getAbsolutePath() + "] because the application is not allowed to read the directory");
            }
            return Collections.emptySet();
        }
        String fullPattern = StringUtils.replace(rootDir.getAbsolutePath(), File.separator, "/");
        if (!pattern.startsWith("/")) {
            fullPattern += "/";
        }
        fullPattern = fullPattern + StringUtils.replace(pattern, File.separator, "/");
        Set<File> result = new LinkedHashSet<File>(8);
        doRetrieveMatchingFiles(fullPattern, rootDir, result);
        return result;
    }


    protected void doRetrieveMatchingFiles(String fullPattern, File dir, Set<File> result) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Searching directory [" + dir.getAbsolutePath() + "] for files matching pattern [" + fullPattern + "]");
        }
        File[] dirContents = dir.listFiles();
        if (dirContents == null) {
            if (logger.isWarnEnabled()) {
                logger.warning("Could not retrieve contents of directory [" + dir.getAbsolutePath() + "]");
            }
            return;
        }
        for (File content : dirContents) {
            String currPath = StringUtils.replace(content.getAbsolutePath(), File.separator, "/");
            if (content.isDirectory() && getPathMatcher().matchStart(fullPattern, currPath + "/")) {
                if (!content.canRead()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Skipping subdirectory [" + dir.getAbsolutePath() + "] because the application is not allowed to read the directory");
                    }
                } else {
                    doRetrieveMatchingFiles(fullPattern, content, result);
                }
            }
            if (getPathMatcher().match(fullPattern, currPath)) {
                result.add(content);
            }
        }
    }


    private static class VfsResourceMatchingDelegate {

        public static Set<Resource> findMatchingResources(Resource rootResource, String locationPattern, PathMatcher pathMatcher) throws IOException {
            Object root = VfsPatternUtils.findRoot(rootResource.getURL());
            PatternVirtualFileVisitor visitor = new PatternVirtualFileVisitor(VfsPatternUtils.getPath(root), locationPattern, pathMatcher);
            VfsPatternUtils.visit(root, visitor);
            return visitor.getResources();
        }
    }


    @SuppressWarnings("unused")
    private static class PatternVirtualFileVisitor implements InvocationHandler {

        private final String subPattern;

        private final PathMatcher pathMatcher;

        private final String rootPath;

        private final Set<Resource> resources = new LinkedHashSet<Resource>();

        public PatternVirtualFileVisitor(String rootPath, String subPattern, PathMatcher pathMatcher) {
            this.subPattern = subPattern;
            this.pathMatcher = pathMatcher;
            this.rootPath = (rootPath.length() == 0 || rootPath.endsWith("/") ? rootPath : rootPath + "/");
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (Object.class == method.getDeclaringClass()) {
                if (methodName.equals("equals")) {

                    return (proxy == args[0]);
                } else if (methodName.equals("hashCode")) {
                    return System.identityHashCode(proxy);
                }
            } else if ("getAttributes".equals(methodName)) {
                return getAttributes();
            } else if ("visit".equals(methodName)) {
                visit(args[0]);
                return null;
            } else if ("toString".equals(methodName)) {
                return toString();
            }

            throw new IllegalStateException("Unexpected method invocation: " + method);
        }

        public void visit(Object vfsResource) {
            if (this.pathMatcher.match(this.subPattern, VfsPatternUtils.getPath(vfsResource).substring(this.rootPath.length()))) {
                this.resources.add(new VfsResource(vfsResource));
            }
        }

        public Object getAttributes() {
            return VfsPatternUtils.getVisitorAttribute();
        }

        public Set<Resource> getResources() {
            return this.resources;
        }

        public int size() {
            return this.resources.size();
        }

        @Override
        public String toString() {
            return "sub-pattern: " + this.subPattern + ", resources: " + this.resources;
        }
    }

}