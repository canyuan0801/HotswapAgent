
package org.hotswap.agent.plugin.owb_jakarta.transformer;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.expr.ExprEditor;
import org.hotswap.agent.javassist.expr.FieldAccess;
import org.hotswap.agent.plugin.cdi.HaCdiCommons;


public class AbstractProducerTransformer {

    @OnClassLoadEvent(classNameRegexp = "org.apache.webbeans.portable.AbstractProducer")
    public static void patchProxyFactory(CtClass ctClass, ClassPool classPool) throws NotFoundException, CannotCompileException {
        if (!HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        CtMethod getProxyClassMethod = ctClass.getDeclaredMethod("defineInterceptorStack");
        getProxyClassMethod.instrument(
            new ExprEditor() {
                public void edit(FieldAccess e) throws CannotCompileException {
                    if (e.isWriter() && "methodInterceptors".equals(e.getFieldName())) {
                        e.replace("{ " +
                            "if($0.methodInterceptors==null) $0.methodInterceptors=new java.util.HashMap();" +
                            "$0.methodInterceptors.clear();" +
                            "$0.methodInterceptors.putAll($1);" +
                        "}");
                    }
                }
            });
    }

}
