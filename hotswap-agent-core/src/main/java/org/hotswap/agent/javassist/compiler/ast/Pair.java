

package org.hotswap.agent.javassist.compiler.ast;

import org.hotswap.agent.javassist.compiler.CompileError;


public class Pair extends ASTree {

    private static final long serialVersionUID = 1L;
    protected ASTree left, right;

    public Pair(ASTree _left, ASTree _right) {
        left = _left;
        right = _right;
    }

    @Override
    public void accept(Visitor v) throws CompileError { v.atPair(this); }

    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("(<Pair> ");
        sbuf.append(left == null ? "<null>" : left.toString());
        sbuf.append(" . ");
        sbuf.append(right == null ? "<null>" : right.toString());
        sbuf.append(')');
        return sbuf.toString();
    }

    @Override
    public ASTree getLeft() { return left; }

    @Override
    public ASTree getRight() { return right; }

    @Override
    public void setLeft(ASTree _left) { left = _left; }

    @Override
    public void setRight(ASTree _right) { right = _right; }
}
