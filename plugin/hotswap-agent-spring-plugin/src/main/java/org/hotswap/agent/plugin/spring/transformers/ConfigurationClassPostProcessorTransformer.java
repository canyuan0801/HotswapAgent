
package org.hotswap.agent.plugin.spring.transformers;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.expr.ExprEditor;
import org.hotswap.agent.javassist.expr.MethodCall;
import org.hotswap.agent.logging.AgentLogger;

public class ConfigurationClassPostProcessorTransformer {
    private static final AgentLogger LOGGER = AgentLogger.getLogger(ConfigurationClassPostProcessorTransformer.class);

    @OnClassLoadEvent(classNameRegexp = "org.springframework.context.annotation.ConfigurationClassPostProcessor")
    public static void transform(CtClass clazz, ClassPool classPool) throws NotFoundException, CannotCompileException {
        LOGGER.debug("Class 'org.springframework.context.annotation.ConfigurationClassPostProcessor' patched with processor registration.");
        CtMethod method = clazz.getDeclaredMethod("processConfigBeanDefinitions",
                new CtClass[]{classPool.get("org.springframework.beans.factory.support.BeanDefinitionRegistry")});
        method.insertAfter("org.hotswap.agent.plugin.spring.core.ConfigurationClassPostProcessorEnhance.getInstance($1)." +
                "setProcessor(this);");
        try {

            CtMethod enhanceConfigurationClassesMethod = clazz.getDeclaredMethod("enhanceConfigurationClasses");
            enhanceConfigurationClassesMethod.instrument(new ExprEditor() {
                        @Override
                        public void edit(MethodCall m) throws CannotCompileException {
                            if (m.getClassName().equals("org.springframework.beans.factory.config.ConfigurableListableBeanFactory")
                                    && m.getMethodName().equals("containsSingleton")) {
                                m.replace("{$_ = $proceed($$) && " +
                                        "(org.hotswap.agent.plugin.spring.reload.BeanFactoryAssistant.getBeanFactoryAssistant($0) == null || " +
                                        "!org.hotswap.agent.plugin.spring.reload.BeanFactoryAssistant.getBeanFactoryAssistant($0).isReload());}");
                            }
                        }
                    });
        } catch (NotFoundException e) {

        }

    }
}
