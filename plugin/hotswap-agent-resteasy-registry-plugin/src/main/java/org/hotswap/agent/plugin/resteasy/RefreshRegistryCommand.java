
package org.hotswap.agent.plugin.resteasy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.ws.rs.Path;

import org.hotswap.agent.command.MergeableCommand;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.logging.AgentLogger.Level;
import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.core.registry.RootClassNode;
import org.jboss.resteasy.core.registry.RootNode;
import org.jboss.resteasy.plugins.server.servlet.ServletContainerDispatcher;
import org.jboss.resteasy.spi.Registry;


public class RefreshRegistryCommand extends MergeableCommand {

    private static AgentLogger LOGGER = AgentLogger.getLogger(RefreshRegistryCommand.class);

    private ClassLoader classLoader;

    private ServletContext context;

    private String className;

    private ServletContainerDispatcher servletContainerDispatcher;

    private Class<?> original;

    public void setupCmd(ClassLoader classLoader, Object context, Object servletContainerDispatcher, String className, Class<?> original) {
        this.classLoader = classLoader;
        this.context = (ServletContext) context;
        this.servletContainerDispatcher = (ServletContainerDispatcher) servletContainerDispatcher;
        this.className = className;
        this.original = original;
    }

    @Override
    public void executeCommand() {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        LOGGER.debug("Re-Loading class: {} , {} , {}", className, oldClassLoader, classLoader);

        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            Registry registry = (Registry) context.getAttribute(Registry.class.getName());
            if (registry == null) {
                registry = servletContainerDispatcher.getDispatcher().getRegistry();
            }

            if(original != null) {
                registry.removeRegistrations(original);
            }

            Class<?> c = classLoader.loadClass(className);


            registry.removeRegistrations(c);


            if (registry instanceof ResourceMethodRegistry) {
                ResourceMethodRegistry rm = ResourceMethodRegistry.class.cast(registry);
                Map<String, List<ResourceInvoker>> bounded = rm.getBounded();
                for (Entry<String, List<ResourceInvoker>> e : bounded.entrySet()) {
                    LOGGER.debug("Examining {}", e.getKey());
                    for (ResourceInvoker r : e.getValue()) {
                        if(LOGGER.isLevelEnabled(Level.DEBUG)){
                            LOGGER.debug("Examining {} for method {} in class {}", e.getKey(), r.getMethod().getName(),
                                    r.getMethod().getDeclaringClass());
                        }
                        if (r.getMethod().getDeclaringClass().getName().equals(className)) {
                            removeRegistration(rm, e.getKey(), r.getMethod());
                        }
                    }
                }
            }


            registry.addPerRequestResource(c);
        } catch (Exception e) {
            LOGGER.error("Could not reload rest class {}", e, className);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T get(ResourceMethodRegistry rm, String field) {
        Class<?> c;
        try {
            c = classLoader.loadClass(ResourceMethodRegistry.class.getName());
            Field f = c.getField(field);
            return (T) f.get(rm);
        } catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException
                | IllegalAccessException e) {
            LOGGER.error("Could not get field {}", e, field);
        }

        return null;
    }

    private void removeRegistration(ResourceMethodRegistry rm, String path, Method method) {
        try {
            if (rm.isWiderMatching()) {
                RootNode rootNode = get(rm, "rootNode");
                rootNode.removeBinding(path, method);
            } else {
                String methodpath = method.getAnnotation(Path.class).value();
                String classExpression = path.replace(methodpath, "");
                if (classExpression.endsWith("/")) {
                    classExpression.substring(0, classExpression.length() - 1);
                }
                RootClassNode root = get(rm, "root");
                root.removeBinding(classExpression, path, method);
            }
        } catch (Exception e) {
            LOGGER.error("Could not remove method registration from path {}, {}", e, path, method);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((classLoader == null) ? 0 : classLoader.hashCode());
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RefreshRegistryCommand other = (RefreshRegistryCommand) obj;
        if (classLoader == null) {
            if (other.classLoader != null)
                return false;
        } else if (!classLoader.equals(other.classLoader))
            return false;
        if (className == null) {
            if (other.className != null)
                return false;
        } else if (!className.equals(other.className))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "RefreshRegistryCommand [classLoader=" + classLoader + ", className=" + className + "]";
    }
}