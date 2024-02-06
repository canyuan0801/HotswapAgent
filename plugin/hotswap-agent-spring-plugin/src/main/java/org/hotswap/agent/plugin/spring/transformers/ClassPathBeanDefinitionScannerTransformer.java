
package org.hotswap.agent.plugin.spring.transformers;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.spring.SpringPlugin;


public class ClassPathBeanDefinitionScannerTransformer {
    private static final AgentLogger LOGGER = AgentLogger.getLogger(ClassPathBeanDefinitionScannerTransformer.class);


    @OnClassLoadEvent(classNameRegexp = "org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider")
    public static void transform(CtClass clazz, ClassPool classPool) throws NotFoundException, CannotCompileException {
        if (SpringPlugin.basePackagePrefixes == null) {
            CtMethod method = clazz.getDeclaredMethod("findCandidateComponents", new CtClass[]{classPool.get("java.lang.String")});
            method.insertAfter(
                    "if (this instanceof org.springframework.context.annotation.ClassPathBeanDefinitionScanner) {" +
                    "  if (org.hotswap.agent.plugin.spring.scanner.ClassPathBeanDefinitionScannerAgent.getInstance($1) == null) {" +
                    "    org.hotswap.agent.plugin.spring.scanner.ClassPathBeanDefinitionScannerAgent.getInstance(" +
                            "(org.springframework.context.annotation.ClassPathBeanDefinitionScanner)this).registerBasePackage($1);" +
                    "  }" +
                    "}");

            LOGGER.debug("Class 'org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider' " +
                    "patched with basePackage registration.");
        } else {
            LOGGER.debug("No need to register scanned path, instead just register 'spring.basePackagePrefix' in " +
                    "configuration file.");
        }
    }
}
