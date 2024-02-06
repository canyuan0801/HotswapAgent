

package org.hotswap.agent.javassist.compiler.ast;

import org.hotswap.agent.javassist.compiler.CompileError;


public class StringL extends ASTree {
    
    private static final long serialVersionUID = 1L;
    protected String text;

    public StringL(String t) {
        text = t;
    }

    public String get() { return text; }

    @Override
    public String toString() { return "\"" + text + "\""; }

    @Override
    public void accept(Visitor v) throws CompileError { v.atStringL(this); }
}
