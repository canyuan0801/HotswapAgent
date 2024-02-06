
package org.hotswap.agent.plugin.cxf.jaxrs;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.hotswap.agent.command.MergeableCommand;
import org.hotswap.agent.logging.AgentLogger;

import java.util.Objects;


public class CxfJAXRSCommand extends MergeableCommand {

    private static AgentLogger LOGGER = AgentLogger.getLogger(CxfJAXRSCommand.class);

    private ClassLoader classLoader;
    private ClassResourceInfo criProxy;
    private String resourceClassPath;

    public void setupCmd(ClassLoader classLoader, Object criProxy) {
        this.classLoader = classLoader;
        this.criProxy = (ClassResourceInfo) criProxy;
        resourceClassPath = this.criProxy.getServiceClass().toString();
    }

    @Override
    public void executeCommand() {

        LOGGER.debug("Reloading service={}, in classLoader={}", criProxy.getServiceClass(), classLoader);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            ClassResourceInfoProxyHelper.reloadClassResourceInfo(criProxy);
            LOGGER.info("Resource class {} reloaded.", criProxy.getResourceClass().getName());
        } catch (Exception e) {
            LOGGER.error("Could not reload JAXRS service class {}", e, criProxy.getServiceClass());
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        CxfJAXRSCommand that = (CxfJAXRSCommand) object;
        return Objects.equals(resourceClassPath, that.resourceClassPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceClassPath);
    }

    @Override
    public String toString() {
        return "CxfJAXRSCommand[classLoader=" + classLoader + ", service class =" + criProxy.getServiceClass() + "]";
    }
}