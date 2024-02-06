

package org.hotswap.agent.javassist.compiler.ast;

import org.hotswap.agent.javassist.compiler.CompileError;
import org.hotswap.agent.javassist.compiler.TokenId;


public class Expr extends ASTList implements TokenId {



    private static final long serialVersionUID = 1L;
    protected int operatorId;

    Expr(int op, ASTree _head, ASTList _tail) {
        super(_head, _tail);
        operatorId = op;
    }

    Expr(int op, ASTree _head) {
        super(_head);
        operatorId = op;
    }

    public static Expr make(int op, ASTree oprand1, ASTree oprand2) {
        return new Expr(op, oprand1, new ASTList(oprand2));
    }

    public static Expr make(int op, ASTree oprand1) {
        return new Expr(op, oprand1);
    }

    public int getOperator() { return operatorId; }

    public void setOperator(int op) { operatorId = op; }

    public ASTree oprand1() { return getLeft(); }

    public void setOprand1(ASTree expr) {
        setLeft(expr);
    }

    public ASTree oprand2() { return getRight().getLeft(); }

    public void setOprand2(ASTree expr) {
        getRight().setLeft(expr);
    }

    @Override
    public void accept(Visitor v) throws CompileError { v.atExpr(this); }

    public String getName() {
        int id = operatorId;
        if (id < 128)
            return String.valueOf((char)id);
        else if (NEQ <= id && id <= ARSHIFT_E)
            return opNames[id - NEQ];
        else if (id == INSTANCEOF)
            return "instanceof";
        else
            return String.valueOf(id);
    }

    @Override
    protected String getTag() {
        return "op:" + getName();
    }
}
