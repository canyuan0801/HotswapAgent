

package org.hotswap.agent.javassist.compiler.ast;

import org.hotswap.agent.javassist.compiler.CompileError;
import org.hotswap.agent.javassist.compiler.MemberResolver;
import org.hotswap.agent.javassist.compiler.TokenId;


public class CallExpr extends Expr {

    private static final long serialVersionUID = 1L;
    private MemberResolver.Method method;

    private CallExpr(ASTree _head, ASTList _tail) {
        super(TokenId.CALL, _head, _tail);
        method = null;
    }

    public void setMethod(MemberResolver.Method m) {
        method = m;
    }

    public MemberResolver.Method getMethod() {
        return method;
    }

    public static CallExpr makeCall(ASTree target, ASTree args) {
        return new CallExpr(target, new ASTList(args));
    }

    @Override
    public void accept(Visitor v) throws CompileError { v.atCallExpr(this); }
}
