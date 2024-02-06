
package org.hotswap.agent.plugin.deltaspike.transformer;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.*;
import org.hotswap.agent.javassist.expr.ExprEditor;
import org.hotswap.agent.javassist.expr.MethodCall;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.cdi.HaCdiCommons;


public class DeltaSpikeProxyTransformer {

    private static AgentLogger LOGGER = AgentLogger.getLogger(DeltaSpikeProxyTransformer.class);


    @OnClassLoadEvent(classNameRegexp = "org.apache.deltaspike.proxy.api.DeltaSpikeProxyFactory")
    public static void patchDeltaSpikeProxyFactory(ClassPool classPool, CtClass ctClass) throws NotFoundException, CannotCompileException {
        if (HaCdiCommons.isJakarta(classPool)) {
            return;
        }

        instrumentTryToLoadClassForName(ctClass, "getProxyClass");
        instrumentTryToLoadClassForName(ctClass, "createProxyClass");

        instrumentTryToLoadClassForName(ctClass, "resolveAlreadyDefinedProxyClass");
    }

    private static void instrumentTryToLoadClassForName(CtClass ctClass, String methodName) throws CannotCompileException {
        try {
            CtMethod getProxyClassMethod = ctClass.getDeclaredMethod(methodName);
            getProxyClassMethod.instrument(
                    new ExprEditor() {
                        public void edit(MethodCall m) throws CannotCompileException {
                            if (m.getClassName().equals("org.apache.deltaspike.core.util.ClassUtils") && m.getMethodName().equals("tryToLoadClassForName"))
                                m.replace("{ $_ = org.hotswap.agent.plugin.deltaspike.command.ProxyClassLoadingDelegate.tryToLoadClassForName($$); }");
                        }
                    });
        } catch (NotFoundException e) {
            LOGGER.debug("Method '{}' not found in '{}'.", methodName, ctClass.getName());
        }
    }


    @OnClassLoadEvent(classNameRegexp = "org.apache.deltaspike.proxy.impl.AsmProxyClassGenerator")
    public static void patchAsmProxyClassGenerator(ClassPool classPool, CtClass ctClass) throws NotFoundException, CannotCompileException {
        if (HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        CtMethod generateProxyClassMethod = ctClass.getDeclaredMethod("generateProxyClass");
        generateProxyClassMethod.instrument(
                new ExprEditor() {
                    public void edit(MethodCall m) throws CannotCompileException {
                        if (m.getClassName().equals("org.apache.deltaspike.proxy.impl.AsmProxyClassGenerator") && m.getMethodName().equals("loadClass"))
                            m.replace("{ $_ = org.hotswap.agent.plugin.deltaspike.command.ProxyClassLoadingDelegate.loadClass($$); }");
                    }
                });
        LOGGER.debug("org.apache.deltaspike.proxy.impl.AsmProxyClassGenerator patched.");
    }


    @OnClassLoadEvent(classNameRegexp = "org.apache.deltaspike.proxy.impl.AsmDeltaSpikeProxyClassGenerator")
    public static void patchAsmDeltaSpikeProxyClassGenerator(ClassPool classPool, CtClass ctClass) throws NotFoundException, CannotCompileException {
        if (HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        CtMethod generateProxyClassMethod = ctClass.getDeclaredMethod("generateProxyClass");
        generateProxyClassMethod.instrument(
                new ExprEditor() {
                    public void edit(MethodCall m) throws CannotCompileException {
                        if (m.getClassName().equals("org.apache.deltaspike.proxy.impl.AsmDeltaSpikeProxyClassGenerator") && m.getMethodName().equals("loadClass")) {
                            m.replace("{ $_ = org.hotswap.agent.plugin.deltaspike.command.ProxyClassLoadingDelegate.loadClass($$); }");
                        } else if (m.getClassName().equals("org.apache.deltaspike.proxy.impl.ClassDefiner") && m.getMethodName().equals("defineClass")) {
                            m.replace("{ $_ = org.hotswap.agent.plugin.deltaspike.command.ProxyClassLoadingDelegate.defineClass($$); }");
                        }
                    }
                });
        LOGGER.debug("org.apache.deltaspike.proxy.impl.AsmDeltaSpikeProxyClassGenerator patched.");
    }


}
