
package org.hotswap.agent.plugin.spring.transformers;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.*;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.spring.files.XmlBeanDefinitionScannerAgent;


public class XmlBeanDefinitionScannerTransformer {
    private static AgentLogger LOGGER = AgentLogger.getLogger(XmlBeanDefinitionScannerTransformer.class);


    @OnClassLoadEvent(classNameRegexp = "org.springframework.beans.factory.xml.XmlBeanDefinitionReader")
    public static void transform(CtClass clazz, ClassPool classPool) throws NotFoundException, CannotCompileException {

        CtMethod method = clazz.getDeclaredMethod("registerBeanDefinitions", new CtClass[]{
                classPool.get("org.w3c.dom.Document"),
                classPool.get("org.springframework.core.io.Resource")});
        method.insertBefore(XmlBeanDefinitionScannerAgent.class.getName() + ".registerXmlBeanDefinitionScannerAgent(this, $2);");
        LOGGER.debug("Class 'org.springframework.beans.factory.xml.XmlBeanDefinitionReader' patched with xmlReader registration.");
    }
}
