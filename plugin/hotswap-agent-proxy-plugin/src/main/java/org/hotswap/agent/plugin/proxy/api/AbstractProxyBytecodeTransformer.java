
package org.hotswap.agent.plugin.proxy.api;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtField;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.Modifier;


public abstract class AbstractProxyBytecodeTransformer implements ProxyBytecodeTransformer
{
    private ClassPool classPool;


    public AbstractProxyBytecodeTransformer(ClassPool classPool) {
        this.classPool = classPool;
    }

    public byte[] transform(byte[] byteCode) throws Exception {
        CtClass cc = classPool.makeClass(new ByteArrayInputStream(byteCode), false);
        try {
            String initFieldName = INIT_FIELD_PREFIX + generateRandomString();
            addStaticInitStateField(cc, initFieldName);

            String initCode = getInitCall(cc, initFieldName);

            addInitCallToMethods(cc, initFieldName, initCode);
            return cc.toBytecode();
        } finally {
            cc.detach();
        }
    }


    protected abstract String getInitCall(CtClass cc, String random) throws Exception;

    protected String generateRandomString() {
        return UUID.randomUUID().toString().replace("-", "");
    }


    protected void addInitCallToMethods(CtClass cc, String clinitFieldName, String initCall) throws Exception {
        CtMethod[] methods = cc.getDeclaredMethods();
        for (CtMethod ctMethod : methods) {
            if (!ctMethod.isEmpty() && !Modifier.isStatic(ctMethod.getModifiers())) {
                ctMethod.insertBefore("if(!" + clinitFieldName + "){synchronized(" + cc.getName() + ".class){if(!"
                        + clinitFieldName + "){" + initCall + "}}}");
            }
        }
    }


    protected void addStaticInitStateField(CtClass cc, String clinitFieldName) throws Exception {
        CtField f = new CtField(CtClass.booleanType, clinitFieldName, cc);
        f.setModifiers(Modifier.PRIVATE | Modifier.STATIC);

        cc.addField(f, "true");
    }
}
