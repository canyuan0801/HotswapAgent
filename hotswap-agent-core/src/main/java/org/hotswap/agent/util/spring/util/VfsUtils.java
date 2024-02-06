

package org.hotswap.agent.util.spring.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;

import org.hotswap.agent.logging.AgentLogger;


public abstract class VfsUtils {
    
    private static final AgentLogger LOGGER = AgentLogger.getLogger(VfsUtils.class);
    
    private static final String VFS3_PKG = "org.jboss.vfs.";
    private static final String VFS_NAME = "VFS";

    private static Method VFS_METHOD_GET_ROOT_URL = null;
    private static Method VFS_METHOD_GET_ROOT_URI = null;

    private static Method VIRTUAL_FILE_METHOD_EXISTS = null;
    private static Method VIRTUAL_FILE_METHOD_GET_INPUT_STREAM;
    private static Method VIRTUAL_FILE_METHOD_GET_SIZE;
    private static Method VIRTUAL_FILE_METHOD_GET_LAST_MODIFIED;
    private static Method VIRTUAL_FILE_METHOD_TO_URL;
    private static Method VIRTUAL_FILE_METHOD_TO_URI;
    private static Method VIRTUAL_FILE_METHOD_GET_NAME;
    private static Method VIRTUAL_FILE_METHOD_GET_PATH_NAME;
    private static Method VIRTUAL_FILE_METHOD_GET_CHILD;

    protected static Class<?> VIRTUAL_FILE_VISITOR_INTERFACE;
    protected static Method VIRTUAL_FILE_METHOD_VISIT;

    private static Field VISITOR_ATTRIBUTES_FIELD_RECURSE = null;
    private static Method GET_PHYSICAL_FILE = null;

    private static volatile boolean initialized = false;

    static {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = VfsUtils.class.getClassLoader();
        }
        try {
            Class<?> vfsClass = loader.loadClass(VFS3_PKG + VFS_NAME);
            VFS_METHOD_GET_ROOT_URL = ReflectionUtils.findMethod(vfsClass, "getChild", URL.class);
            VFS_METHOD_GET_ROOT_URI = ReflectionUtils.findMethod(vfsClass, "getChild", URI.class);

            Class<?> virtualFile = loader.loadClass(VFS3_PKG + "VirtualFile");
            VIRTUAL_FILE_METHOD_EXISTS = ReflectionUtils.findMethod(virtualFile, "exists");
            VIRTUAL_FILE_METHOD_GET_INPUT_STREAM = ReflectionUtils.findMethod(virtualFile, "openStream");
            VIRTUAL_FILE_METHOD_GET_SIZE = ReflectionUtils.findMethod(virtualFile, "getSize");
            VIRTUAL_FILE_METHOD_GET_LAST_MODIFIED = ReflectionUtils.findMethod(virtualFile, "getLastModified");
            VIRTUAL_FILE_METHOD_TO_URI = ReflectionUtils.findMethod(virtualFile, "toURI");
            VIRTUAL_FILE_METHOD_TO_URL = ReflectionUtils.findMethod(virtualFile, "toURL");
            VIRTUAL_FILE_METHOD_GET_NAME = ReflectionUtils.findMethod(virtualFile, "getName");
            VIRTUAL_FILE_METHOD_GET_PATH_NAME = ReflectionUtils.findMethod(virtualFile, "getPathName");
            GET_PHYSICAL_FILE = ReflectionUtils.findMethod(virtualFile, "getPhysicalFile");
            VIRTUAL_FILE_METHOD_GET_CHILD = ReflectionUtils.findMethod(virtualFile, "getChild", String.class);

            VIRTUAL_FILE_VISITOR_INTERFACE = loader.loadClass(VFS3_PKG + "VirtualFileVisitor");
            VIRTUAL_FILE_METHOD_VISIT = ReflectionUtils.findMethod(virtualFile, "visit", VIRTUAL_FILE_VISITOR_INTERFACE);

            Class<?> visitorAttributesClass = loader.loadClass(VFS3_PKG + "VisitorAttributes");
            VISITOR_ATTRIBUTES_FIELD_RECURSE = ReflectionUtils.findField(visitorAttributesClass, "RECURSE");
            initialized = true;
        } catch (ClassNotFoundException ex) {
            LOGGER.error("Could not detect JBoss VFS infrastructure", ex);
        }
    }

    private static void init() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = VfsUtils.class.getClassLoader();
        }
        try {
            Class<?> vfsClass = loader.loadClass(VFS3_PKG + VFS_NAME);
            VFS_METHOD_GET_ROOT_URL = ReflectionUtils.findMethod(vfsClass, "getChild", URL.class);
            VFS_METHOD_GET_ROOT_URI = ReflectionUtils.findMethod(vfsClass, "getChild", URI.class);

            Class<?> virtualFile = loader.loadClass(VFS3_PKG + "VirtualFile");
            VIRTUAL_FILE_METHOD_EXISTS = ReflectionUtils.findMethod(virtualFile, "exists");
            VIRTUAL_FILE_METHOD_GET_INPUT_STREAM = ReflectionUtils.findMethod(virtualFile, "openStream");
            VIRTUAL_FILE_METHOD_GET_SIZE = ReflectionUtils.findMethod(virtualFile, "getSize");
            VIRTUAL_FILE_METHOD_GET_LAST_MODIFIED = ReflectionUtils.findMethod(virtualFile, "getLastModified");
            VIRTUAL_FILE_METHOD_TO_URI = ReflectionUtils.findMethod(virtualFile, "toURI");
            VIRTUAL_FILE_METHOD_TO_URL = ReflectionUtils.findMethod(virtualFile, "toURL");
            VIRTUAL_FILE_METHOD_GET_NAME = ReflectionUtils.findMethod(virtualFile, "getName");
            VIRTUAL_FILE_METHOD_GET_PATH_NAME = ReflectionUtils.findMethod(virtualFile, "getPathName");
            GET_PHYSICAL_FILE = ReflectionUtils.findMethod(virtualFile, "getPhysicalFile");
            VIRTUAL_FILE_METHOD_GET_CHILD = ReflectionUtils.findMethod(virtualFile, "getChild", String.class);

            VIRTUAL_FILE_VISITOR_INTERFACE = loader.loadClass(VFS3_PKG + "VirtualFileVisitor");
            VIRTUAL_FILE_METHOD_VISIT = ReflectionUtils.findMethod(virtualFile, "visit", VIRTUAL_FILE_VISITOR_INTERFACE);

            Class<?> visitorAttributesClass = loader.loadClass(VFS3_PKG + "VisitorAttributes");
            VISITOR_ATTRIBUTES_FIELD_RECURSE = ReflectionUtils.findField(visitorAttributesClass, "RECURSE");
            initialized = true;
        } catch (ClassNotFoundException ex) {
            LOGGER.error("Could not detect JBoss VFS infrastructure", ex);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> T invokeVfsMethod(Method method, Object target, Object... args) throws IOException {
        try {
            return (T) method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            Throwable targetEx = ex.getTargetException();
            if (targetEx instanceof IOException) {
                throw (IOException) targetEx;
            }
            ReflectionUtils.handleInvocationTargetException(ex);
        } catch (Exception ex) {
            ReflectionUtils.handleReflectionException(ex);
        }

        throw new IllegalStateException("Invalid code path reached");
    }

    public static boolean exists(Object vfsResource) {
        if (!initialized) {
            init();
        }
        try {
            return (Boolean) invokeVfsMethod(VIRTUAL_FILE_METHOD_EXISTS, vfsResource);
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean isReadable(Object vfsResource) {
        if (!initialized) {
            init();
        }
        try {
            return ((Long) invokeVfsMethod(VIRTUAL_FILE_METHOD_GET_SIZE, vfsResource) > 0);
        } catch (IOException ex) {
            return false;
        }
    }

    public static long getSize(Object vfsResource) throws IOException {
        if (!initialized) {
            init();
        }
        return (Long) invokeVfsMethod(VIRTUAL_FILE_METHOD_GET_SIZE, vfsResource);
    }

    public static long getLastModified(Object vfsResource) throws IOException {
        if (!initialized) {
            init();
        }
        return (Long) invokeVfsMethod(VIRTUAL_FILE_METHOD_GET_LAST_MODIFIED, vfsResource);
    }

    public static InputStream getInputStream(Object vfsResource) throws IOException {
        if (!initialized) {
            init();
        }
        return invokeVfsMethod(VIRTUAL_FILE_METHOD_GET_INPUT_STREAM, vfsResource);
    }

    public static URL getURL(Object vfsResource) throws IOException {
        if (!initialized) {
            init();
        }
        return (URL) invokeVfsMethod(VIRTUAL_FILE_METHOD_TO_URL, vfsResource);
    }

    public static URI getURI(Object vfsResource) throws IOException {
        if (!initialized) {
            init();
        }
        return (URI) invokeVfsMethod(VIRTUAL_FILE_METHOD_TO_URI, vfsResource);
    }

    public static String getName(Object vfsResource) {
        if (!initialized) {
            init();
        }
        try {
            return (String) invokeVfsMethod(VIRTUAL_FILE_METHOD_GET_NAME, vfsResource);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot get resource name", ex);
        }
    }

    public static <T> T getRelative(URL url) throws IOException {
        if (!initialized) {
            init();
        }
        return invokeVfsMethod(VFS_METHOD_GET_ROOT_URL, null, url);
    }

    public static <T> T getChild(Object vfsResource, String path) throws IOException {
        if (!initialized) {
            init();
        }
        return invokeVfsMethod(VIRTUAL_FILE_METHOD_GET_CHILD, vfsResource, path);
    }

    public static File getFile(Object vfsResource) throws IOException {
        if (!initialized) {
            init();
        }
        return (File) invokeVfsMethod(GET_PHYSICAL_FILE, vfsResource);
    }

    public static <T> T getRoot(URI url) throws IOException {
        if (!initialized) {
            init();
        }
        return invokeVfsMethod(VFS_METHOD_GET_ROOT_URI, null, url);
    }

    
    public static <T> T getRoot(URL url) throws IOException {
        if (!initialized) {
            init();
        }
        return invokeVfsMethod(VFS_METHOD_GET_ROOT_URL, null, url);
    }

    protected static <T> T doGetVisitorAttribute() {
        if (!initialized) {
            init();
        }
        return ReflectionUtils.getField(VISITOR_ATTRIBUTES_FIELD_RECURSE, null);
    }

    protected static String doGetPath(Object resource) {
        if (!initialized) {
            init();
        }
        return (String) ReflectionUtils.invokeMethod(VIRTUAL_FILE_METHOD_GET_PATH_NAME, resource);
    }

}