
package org.hotswap.agent.plugin.weld.transformer;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.CtNewMethod;
import org.hotswap.agent.javassist.Modifier;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.expr.ExprEditor;
import org.hotswap.agent.javassist.expr.MethodCall;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.cdi.HaCdiCommons;
import org.hotswap.agent.plugin.weld.WeldPlugin;
import org.hotswap.agent.util.PluginManagerInvoker;


public class ProxyFactoryTransformer {

    private static AgentLogger LOGGER = AgentLogger.getLogger(ProxyFactoryTransformer.class);


    @OnClassLoadEvent(classNameRegexp = "org.jboss.weld.bean.proxy.ProxyFactory")
    public static void patchProxyFactory(ClassPool classPool, CtClass ctClass) throws NotFoundException, CannotCompileException {
        if (HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        CtClass[] constructorParams = new CtClass[] {
            classPool.get("java.lang.String"),
            classPool.get("java.lang.Class"),
            classPool.get("java.util.Set"),

            classPool.get("javax.enterprise.inject.spi.Bean"),
            classPool.get("boolean")
        };

        CtConstructor declaredConstructor = ctClass.getDeclaredConstructor(constructorParams);


        declaredConstructor.insertAfter("{" +
            "java.lang.Class originalClass = (this.bean != null) ? this.bean.getBeanClass() : this.proxiedBeanType;" +
            "java.lang.ClassLoader loader = originalClass.getClassLoader();" +
            "if (loader==null) {"+
                "loader = Thread.currentThread().getContextClassLoader();" +
            "}" +
            "if (" + PluginManager.class.getName() + ".getInstance().isPluginInitialized(\"" + WeldPlugin.class.getName() + "\", loader)) {" +
                PluginManagerInvoker.buildCallPluginMethod("loader", WeldPlugin.class, "registerProxyFactory",
                        "this", "java.lang.Object",
                        "bean", "java.lang.Object",
                        "loader", "java.lang.ClassLoader",
                        "proxiedBeanType", "java.lang.Class"
                ) +
            "}" +
        "}");

        try {

            CtMethod oldMethod = ctClass.getDeclaredMethod("toClass");
            oldMethod.setName("$$ha$toClass");
            oldMethod.setModifiers(Modifier.PUBLIC);
            CtMethod newMethod = CtNewMethod.make(
                    "protected java.lang.Class toClass(org.jboss.classfilewriter.ClassFile ct, java.lang.Class originalClass, " +
                                "org.jboss.weld.serialization.spi.ProxyServices proxyServices, java.security.ProtectionDomain domain) {" +
                        "return  org.hotswap.agent.plugin.weld.command.ProxyClassLoadingDelegate.toClassWeld3(this, ct, originalClass, proxyServices, domain);" +
                     "}", ctClass);
            ctClass.addMethod(newMethod);
        } catch (NotFoundException e) {

            CtMethod getProxyClassMethod = ctClass.getDeclaredMethod("getProxyClass");
            getProxyClassMethod.instrument(
                    new ExprEditor() {
                        public void edit(MethodCall m) throws CannotCompileException {
                            if (m.getClassName().equals(ClassLoader.class.getName()) && m.getMethodName().equals("loadClass"))
                                m.replace("{ $_ = org.hotswap.agent.plugin.weld.command.ProxyClassLoadingDelegate.loadClass(this.classLoader,$1); }");
                        }
                    });

            CtMethod createProxyClassMethod = ctClass.getDeclaredMethod("createProxyClass");
            createProxyClassMethod.instrument(
                    new ExprEditor() {
                        public void edit(MethodCall m) throws CannotCompileException {

                            if (m.getClassName().equals("org.jboss.weld.util.bytecode.ClassFileUtils") && m.getMethodName().equals("toClass"))
                                try {
                                    if (m.getMethod().getParameterTypes().length == 3) {
                                        m.replace("{ $_ = org.hotswap.agent.plugin.weld.command.ProxyClassLoadingDelegate.toClassWeld2($$); }");
                                    } else if (m.getMethod().getParameterTypes().length == 4) {
                                        LOGGER.debug("Proxy factory patch for delegating method skipped.", m.getClassName(), m.getMethodName());
                                    } else {
                                        LOGGER.error("Method '{}.{}' patch failed. Unknown method arguments.", m.getClassName(), m.getMethodName());
                                    }
                                } catch (NotFoundException e) {
                                    LOGGER.error("Method '{}' not found in '{}'.", m.getMethodName(), m.getClassName());
                            }
                        }
                    }
            );
        }
    }

}
