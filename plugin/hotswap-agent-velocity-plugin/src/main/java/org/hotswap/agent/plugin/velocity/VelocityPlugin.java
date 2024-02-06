
package org.hotswap.agent.plugin.velocity;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;


@Plugin(name = "Velocity",
        description = "Enhance org.springframework.ui.velocity.VelocityEngineFactory",
        testedVersions = {"4.3.8.RELEASE"},
        expectedVersions = {"4.3.8.RELEASE"}
)
public class VelocityPlugin {
    private static final AgentLogger LOGGER = AgentLogger.getLogger(VelocityPlugin.class);

    @OnClassLoadEvent(classNameRegexp = "org.springframework.ui.velocity.VelocityEngineFactory")
    public static void patchSetPreferFileSystemAccess(ClassPool classPool, final CtClass ctClass) {
        try {
            CtMethod method = ctClass.getDeclaredMethod("setPreferFileSystemAccess", new CtClass[]{classPool.get("boolean")});
            method.insertAfter("this.preferFileSystemAccess = false;");
        } catch (NotFoundException | CannotCompileException e) {
            LOGGER.debug("Cannot patch parseAndProcess method for {}", ctClass.getName(), e);
        }
    }

    @OnClassLoadEvent(classNameRegexp = "org.springframework.ui.velocity.VelocityEngineFactory")
    public static void patchSetResourceLoaderPath(ClassPool classPool, final CtClass ctClass) {
        try {
            CtMethod method = ctClass.getDeclaredMethod("setResourceLoaderPath", new CtClass[]{classPool.get("java.lang.String")});
            method.insertAfter("this.resourceLoaderPath = \"classpath:/$$ha$velocity/,\" + this.resourceLoaderPath;");
        } catch (NotFoundException | CannotCompileException e) {
            LOGGER.debug("Cannot patch parseAndProcess method for {}", ctClass.getName(), e);
        }
    }

    @OnClassLoadEvent(classNameRegexp = "org.springframework.ui.velocity.VelocityEngineFactory")
    public static void patchInitSpringResourceLoader(ClassPool classPool, final CtClass ctClass) {
        try {
            CtMethod method = ctClass.getDeclaredMethod("initSpringResourceLoader", new CtClass[]{classPool.get("org.apache.velocity.app.VelocityEngine"),
                    classPool.get("java.lang.String")});
            method.insertAfter("$1.setProperty(\"spring.resource.loader.cache\", \"false\");");
        } catch (NotFoundException | CannotCompileException e) {
            LOGGER.debug("Cannot patch parseAndProcess method for {}", ctClass.getName(), e);
        }
    }
}
