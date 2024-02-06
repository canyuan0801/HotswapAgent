

package org.hotswap.agent.javassist.expr;

import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.MethodInfo;


public class ConstructorCall extends MethodCall {
    
    protected ConstructorCall(int pos, CodeIterator i, CtClass decl, MethodInfo m) {
        super(pos, i, decl, m);
    }

    
    @Override
    public String getMethodName() {
        return isSuper() ? "super" : "this";
    }

    
    @Override
    public CtMethod getMethod() throws NotFoundException {
        throw new NotFoundException("this is a constructor call.  Call getConstructor().");
    }

    
    public CtConstructor getConstructor() throws NotFoundException {
        return getCtClass().getConstructor(getSignature());
    }

    
    @Override
    public boolean isSuper() {
        return super.isSuper();
    }
}
