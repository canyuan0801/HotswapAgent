

package org.hotswap.agent.javassist.compiler.ast;

import org.hotswap.agent.javassist.compiler.CompileError;


public class Symbol extends ASTree {

    private static final long serialVersionUID = 1L;
    protected String identifier;

    public Symbol(String sym) {
        identifier = sym;
    }

    public String get() { return identifier; }

    @Override
    public String toString() { return identifier; }

    @Override
    public void accept(Visitor v) throws CompileError { v.atSymbol(this); }
}
