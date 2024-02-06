

package org.hotswap.agent.javassist.compiler.ast;

import org.hotswap.agent.javassist.compiler.CompileError;


public class ArrayInit extends ASTList {

    private static final long serialVersionUID = 1L;

    public ArrayInit(ASTree firstElement) {
        super(firstElement);
    }

    @Override
    public void accept(Visitor v) throws CompileError { v.atArrayInit(this); }

    @Override
    public String getTag() { return "array"; }
}
