
package org.hotswap.agent.plugin.jetty;

import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.config.PluginConfiguration;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.PluginManagerInvoker;
import org.hotswap.agent.util.ReflectionHelper;

import java.lang.reflect.Array;
import java.net.URL;


@Plugin(name = "Jetty", description = "Jetty plugin.",
        testedVersions = {"6.1.26", "7.6.14", "8.1.14", "9.1.2"},
        expectedVersions = {"4x", "5x", "6x", "7x", "8x", "9x"}
)
public class JettyPlugin {
    private static AgentLogger LOGGER = AgentLogger.getLogger(JettyPlugin.class);

    @Init
    PluginConfiguration pluginConfiguration;


    @OnClassLoadEvent(classNameRegexp = "org.eclipse.jetty.webapp.WebXmlConfiguration")
    public static void patchWebXmlConfiguration(CtClass ctClass) throws NotFoundException, CannotCompileException, ClassNotFoundException {

        try {

            CtMethod doStart = ctClass.getDeclaredMethod("configure");


            String src = PluginManagerInvoker.buildInitializePlugin(JettyPlugin.class, "context.getClassLoader()");
            src += PluginManagerInvoker.buildCallPluginMethod("context.getClassLoader()", JettyPlugin.class,
                    "init", "context", "java.lang.Object");

            doStart.insertBefore(src);
        } catch (NotFoundException e) {
            LOGGER.warning("org.eclipse.jetty.webapp.WebAppContext does not contain startContext method. Jetty plugin will be disabled.\n" +
                    "*** This is Ok, Jetty plugin handles only special properties ***");
            return;
        }
    }


    @OnClassLoadEvent(classNameRegexp = "org.mortbay.jetty.webapp.WebXmlConfiguration")
    public static void patchWebXmlConfiguration6x(CtClass ctClass) throws NotFoundException, CannotCompileException, ClassNotFoundException {
        try {

            CtMethod doStart = ctClass.getDeclaredMethod("configureWebApp");


            String src = PluginManagerInvoker.buildInitializePlugin(JettyPlugin.class, "getWebAppContext().getClassLoader()");
            src += PluginManagerInvoker.buildCallPluginMethod("getWebAppContext().getClassLoader()", JettyPlugin.class,
                    "init", "getWebAppContext()", "java.lang.Object");

            doStart.insertBefore(src);
        } catch (NotFoundException e) {
            LOGGER.warning("org.mortbay.jetty.webapp.WebXmlConfiguration does not contain startContext method. Jetty plugin will be disabled.\n" +
                    "*** This is Ok, Jetty plugin handles only special properties ***");
            return;
        }
    }


    @OnClassLoadEvent(classNameRegexp = "(org.mortbay.jetty.webapp.WebAppContext)|(org.eclipse.jetty.webapp.WebAppContext)")
    public static void patchContextHandler6x(CtClass ctClass) throws NotFoundException, CannotCompileException, ClassNotFoundException {


        try {
            ctClass.getDeclaredMethod("doStop").insertBefore(
                    PluginManagerInvoker.buildCallCloseClassLoader("getClassLoader()")
            );
        } catch (NotFoundException e) {
            LOGGER.debug("org.eclipse.jetty.webapp.WebAppContext does not contain doStop() method. Hotswap agent will not be able to free Jetty plugin resources.");
        }
    }


    public void init(Object contextHandler) {


        ClassLoader loader = contextHandler.getClass().getClassLoader();
        Class contextHandlerClass;
        Class resourceClass;
        Class fileResourceClass;
        Class resourceCollectionClass;

        try {
            contextHandlerClass = loader.loadClass("org.eclipse.jetty.server.handler.ContextHandler");
            resourceClass = loader.loadClass("org.eclipse.jetty.util.resource.Resource");
            fileResourceClass = loader.loadClass("org.eclipse.jetty.util.resource.FileResource");
            resourceCollectionClass = loader.loadClass("org.eclipse.jetty.util.resource.ResourceCollection");
        } catch (ClassNotFoundException e) {
            try {
                contextHandlerClass = loader.loadClass("org.mortbay.jetty.handler.ContextHandler");
                resourceClass = loader.loadClass("org.mortbay.resource.Resource");
                fileResourceClass = loader.loadClass("org.mortbay.resource.FileResource");
                resourceCollectionClass = loader.loadClass("org.mortbay.resource.ResourceCollection");
            } catch (ClassNotFoundException e1) {
                LOGGER.error("Unable to load ContextHandler class from contextHandler {} classloader", contextHandler);
                return;
            }
        }

        String version;

        try {
            Object server = ReflectionHelper.invoke(contextHandler, contextHandlerClass, "getServer", new Class[]{});
            version = server.getClass().getPackage().getImplementationVersion();
        } catch (Exception e) {
            version = "unknown [" + e.getMessage() + "]";
        }


        URL[] webappDir = pluginConfiguration.getWebappDir();
        if (webappDir.length > 0) {
            try {
                Object originalBaseResource = ReflectionHelper.invoke(contextHandler, contextHandlerClass,
                        "getBaseResource", new Class[] {});
                Object resourceArray = Array.newInstance(resourceClass, webappDir.length + 1);
                for (int i = 0; i < webappDir.length; i++) {
                    LOGGER.debug("Watching 'webappDir' for changes: {} in Jetty webapp: {}", webappDir[i],
                            contextHandler);
                    Object fileResource = fileResourceClass.getDeclaredConstructor(URL.class).newInstance(webappDir[i]);
                    Array.set(resourceArray, i, fileResource);
                }
                Array.set(resourceArray, webappDir.length, originalBaseResource);
                Object resourceCollection = resourceCollectionClass.getDeclaredConstructor(resourceArray.getClass())
                        .newInstance(resourceArray);

                ReflectionHelper.invoke(contextHandler, contextHandlerClass, "setBaseResource",
                        new Class[] { resourceClass }, resourceCollection);
            } catch (Exception e) {
                LOGGER.error(
                        "Unable to set webappDir to directory '{}' for Jetty webapp {}. This configuration will not work.",
                        e, webappDir[0], contextHandler);
            }
        }

        LOGGER.info("Jetty plugin initialized - Jetty version '{}', context {}", version, contextHandler);
    }
}
