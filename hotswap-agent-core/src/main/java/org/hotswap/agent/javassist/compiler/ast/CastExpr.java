

package org.hotswap.agent.javassist.compiler.ast;

import org.hotswap.agent.javassist.compiler.CompileError;
import org.hotswap.agent.javassist.compiler.TokenId;


public class CastExpr extends ASTList implements TokenId {

    private static final long serialVersionUID = 1L;
    protected int castType;
    protected int arrayDim;

    public CastExpr(ASTList className, int dim, ASTree expr) {
        super(className, new ASTList(expr));
        castType = CLASS;
        arrayDim = dim;
    }

    public CastExpr(int type, int dim, ASTree expr) {
        super(null, new ASTList(expr));
        castType = type;
        arrayDim = dim;
    }


    public int getType() { return castType; }

    public int getArrayDim() { return arrayDim; }

    public ASTList getClassName() { return (ASTList)getLeft(); }

    public ASTree getOprand() { return getRight().getLeft(); }

    public void setOprand(ASTree t) { getRight().setLeft(t); }

    @Override
    public String getTag() { return "cast:" + castType + ":" + arrayDim; }

    @Override
    public void accept(Visitor v) throws CompileError { v.atCastExpr(this); }
}
