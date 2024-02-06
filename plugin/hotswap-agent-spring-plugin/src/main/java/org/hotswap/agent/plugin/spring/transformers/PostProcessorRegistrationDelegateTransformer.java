
package org.hotswap.agent.plugin.spring.transformers;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.*;
import org.hotswap.agent.javassist.expr.ExprEditor;
import org.hotswap.agent.javassist.expr.MethodCall;
import org.hotswap.agent.logging.AgentLogger;

public class PostProcessorRegistrationDelegateTransformer {
    private static AgentLogger LOGGER = AgentLogger.getLogger(PostProcessorRegistrationDelegateTransformer.class);


    @OnClassLoadEvent(classNameRegexp = "org.springframework.context.support.PostProcessorRegistrationDelegate")
    public static void transform(CtClass clazz, ClassPool classPool) throws NotFoundException, CannotCompileException {
        clazz.addField(CtField.make("private static final org.hotswap.agent.logging.AgentLogger LOGGER = " +
                "org.hotswap.agent.logging.AgentLogger.getLogger(org.springframework.context.support.PostProcessorRegistrationDelegate.class);", clazz));

        CtMethod ctMethod = clazz.getDeclaredMethod("invokeBeanFactoryPostProcessors", new CtClass[]{classPool.get("java.util.Collection"),
                classPool.get("org.springframework.beans.factory.config.ConfigurableListableBeanFactory")});
        ctMethod.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getClassName().equals("org.springframework.beans.factory.config.BeanFactoryPostProcessor")
                        && m.getMethodName().equals("postProcessBeanFactory")) {
                    m.replace("{  try{ $_ = $proceed($$); " +
                            "}catch (java.lang.Exception e) {\n" +
                            "                LOGGER.debug(\"Failed to invoke BeanDefinitionRegistryPostProcessor: {}, reason:{}\",\n" +
                            "                        new java.lang.Object[]{$0.getClass().getName(), e.getMessage()});\n" +
                            "            };}");
                }
            }
        });
        LOGGER.debug("class 'org.springframework.beans.factory.config.PlaceholderConfigurerSupport' patched with placeholder keep.");
    }
}
