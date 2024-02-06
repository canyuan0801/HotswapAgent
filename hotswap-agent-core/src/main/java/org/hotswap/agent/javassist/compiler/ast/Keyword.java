

package org.hotswap.agent.javassist.compiler.ast;

import org.hotswap.agent.javassist.compiler.CompileError;


public class Keyword extends ASTree {
    
    private static final long serialVersionUID = 1L;
    protected int tokenId;

    public Keyword(int token) {
        tokenId = token;
    }

    public int get() { return tokenId; }

    @Override
    public String toString() { return "id:" + tokenId; }

    @Override
    public void accept(Visitor v) throws CompileError { v.atKeyword(this); }
}
