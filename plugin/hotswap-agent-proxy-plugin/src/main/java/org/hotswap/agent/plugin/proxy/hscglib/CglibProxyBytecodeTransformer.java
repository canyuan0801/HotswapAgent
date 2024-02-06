
package org.hotswap.agent.plugin.proxy.hscglib;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.plugin.proxy.api.AbstractProxyBytecodeTransformer;


public class CglibProxyBytecodeTransformer
        extends AbstractProxyBytecodeTransformer {
    public CglibProxyBytecodeTransformer(ClassPool classPool) {
        super(classPool);
    }

    @Override
    protected String getInitCall(CtClass cc, String initFieldName)
            throws Exception {
        CtMethod[] methods = cc.getDeclaredMethods();
        StringBuilder strB = new StringBuilder();
        for (CtMethod ctMethod : methods) {
            if (ctMethod.getName().startsWith("CGLIB$STATICHOOK")) {
                ctMethod.insertAfter(initFieldName + "=true;");
                strB.insert(0, ctMethod.getName() + "();");
                break;
            }
        }

        if (strB.length() == 0)
            throw new RuntimeException(
                    "Could not find CGLIB$STATICHOOK method");
        return strB.toString() + "CGLIB$BIND_CALLBACKS(this);";
    }
}
