

package org.hotswap.agent.javassist.compiler.ast;

import org.hotswap.agent.javassist.compiler.CompileError;


public class Variable extends Symbol {
    
    private static final long serialVersionUID = 1L;
    protected Declarator declarator;

    public Variable(String sym, Declarator d) {
        super(sym);
        declarator = d;
    }

    public Declarator getDeclarator() { return declarator; }

    @Override
    public String toString() {
        return identifier + ":" + declarator.getType();
    }

    @Override
    public void accept(Visitor v) throws CompileError { v.atVariable(this); }
}
