

package org.hotswap.agent.javassist.compiler;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.NotFoundException;

public class CompileError extends Exception {

    private static final long serialVersionUID = 1L;
    private Lex lex;
    private String reason;

    public CompileError(String s, Lex l) {
        reason = s;
        lex = l;
    }

    public CompileError(String s) {
        reason = s;
        lex = null;
    }

    public CompileError(CannotCompileException e) {
        this(e.getReason());
    }

    public CompileError(NotFoundException e) {
        this("cannot find " + e.getMessage());
    }

    public Lex getLex() { return lex; }

    @Override
    public String getMessage() {
        return reason;
    }

    @Override
    public String toString() {
        return "compile error: " + reason;
    }
}
