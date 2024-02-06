
package org.hotswap.agent.plugin.owb.transformer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.expr.ExprEditor;
import org.hotswap.agent.javassist.expr.MethodCall;
import org.hotswap.agent.plugin.cdi.HaCdiCommons;


public class ProxyFactoryTransformer {


    @OnClassLoadEvent(classNameRegexp = "org.apache.webbeans.proxy.AbstractProxyFactory")
    public static void patchProxyFactory(CtClass ctClass, ClassPool classPool) throws NotFoundException, CannotCompileException {
        if (HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        CtMethod getProxyClassMethod = ctClass.getDeclaredMethod("getUnusedProxyClassName");
        getProxyClassMethod.instrument(
                new ExprEditor() {
                    public void edit(MethodCall m) throws CannotCompileException {
                        if (m.getClassName().equals(Class.class.getName()) && m.getMethodName().equals("forName"))
                            m.replace("{ $_ = org.hotswap.agent.plugin.owb.command.ProxyClassLoadingDelegate.forName($$); }");
                    }
                });

        CtMethod createProxyClassMethod = ctClass.getDeclaredMethod("createProxyClass", new CtClass[] {
                classPool.get(ClassLoader.class.getName()),
                classPool.get(String.class.getName()),
                classPool.get(Class.class.getName()),
                classPool.get(Method.class.getName() + "[]"),
                classPool.get(Method.class.getName() + "[]"),
                classPool.get(Constructor.class.getName())
            }
        );

        createProxyClassMethod.instrument(
                new ExprEditor() {
                    public void edit(MethodCall m) throws CannotCompileException {
                        if (m.getMethodName().equals("defineAndLoadClass"))
                            if ("org.apache.webbeans.proxy.Unsafe".equals(m.getClassName())) {

                                m.replace("{ $_ = org.hotswap.agent.plugin.owb.command.ProxyClassLoadingDelegate.defineAndLoadClassWithUnsafe(this.unsafe, $$); }");
                            } else {

                                m.replace("{ $_ = org.hotswap.agent.plugin.owb.command.ProxyClassLoadingDelegate.defineAndLoadClass(this, $$); }");
                            }
                    }
                });
    }

}
