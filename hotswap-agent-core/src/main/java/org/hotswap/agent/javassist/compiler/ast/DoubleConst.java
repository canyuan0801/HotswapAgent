

package org.hotswap.agent.javassist.compiler.ast;

import org.hotswap.agent.javassist.compiler.CompileError;
import org.hotswap.agent.javassist.compiler.TokenId;


public class DoubleConst extends ASTree {

    private static final long serialVersionUID = 1L;
    protected double value;
    protected int type;

    public DoubleConst(double v, int tokenId) { value = v; type = tokenId; }

    public double get() { return value; }

    public void set(double v) { value = v; }


    public int getType() { return type; }

    @Override
    public String toString() { return Double.toString(value); }

    @Override
    public void accept(Visitor v) throws CompileError {
        v.atDoubleConst(this);
    }

    public ASTree compute(int op, ASTree right) {
        if (right instanceof IntConst)
            return compute0(op, (IntConst)right);
        else if (right instanceof DoubleConst)
            return compute0(op, (DoubleConst)right);
        else
            return null;
    }

    private DoubleConst compute0(int op, DoubleConst right) {
        int newType;
        if (this.type == TokenId.DoubleConstant
            || right.type == TokenId.DoubleConstant)
            newType = TokenId.DoubleConstant;
        else
            newType = TokenId.FloatConstant;

        return compute(op, this.value, right.value, newType);
    }

    private DoubleConst compute0(int op, IntConst right) {
        return compute(op, this.value, right.value, this.type);
    }

    private static DoubleConst compute(int op, double value1, double value2,
                                       int newType)
    {
        double newValue;
        switch (op) {
        case '+' :
            newValue = value1 + value2;
            break;
        case '-' :
            newValue = value1 - value2;
            break;
        case '*' :
            newValue = value1 * value2;
            break;
        case '/' :
            newValue = value1 / value2;
            break;
        case '%' :
            newValue = value1 % value2;
            break;
        default :
            return null;
        }

        return new DoubleConst(newValue, newType);
    }
}
