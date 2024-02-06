

package org.hotswap.agent.javassist.compiler.ast;

import org.hotswap.agent.javassist.compiler.CompileError;

public class FieldDecl extends ASTList {

    private static final long serialVersionUID = 1L;

    public FieldDecl(ASTree _head, ASTList _tail) {
        super(_head, _tail);
    }

    public ASTList getModifiers() { return (ASTList)getLeft(); }

    public Declarator getDeclarator() { return (Declarator)tail().head(); }

    public ASTree getInit() { return sublist(2).head(); }

    @Override
    public void accept(Visitor v) throws CompileError {
        v.atFieldDecl(this);
    }
}
