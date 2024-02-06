

package org.hotswap.agent.javassist;

import org.hotswap.agent.javassist.compiler.CompileError;


public class CannotCompileException extends Exception {

    private static final long serialVersionUID = 1L;
    private Throwable myCause;


    @Override
    public synchronized Throwable getCause() {
        return (myCause == this ? null : myCause);
    }


    @Override
    public synchronized Throwable initCause(Throwable cause) {
        myCause = cause;
        return this;
    }

    private String message;


    public String getReason() {
        if (message != null)
            return message;
        return this.toString();
    }


    public CannotCompileException(String msg) {
        super(msg);
        message = msg;
        initCause(null);
    }


    public CannotCompileException(Throwable e) {
        super("by " + e.toString());
        message = null;
        initCause(e);
    }


    public CannotCompileException(String msg, Throwable e) {
        this(msg);
        initCause(e);
    }


    public CannotCompileException(NotFoundException e) {
        this("cannot find " + e.getMessage(), e);
    }


    public CannotCompileException(CompileError e) {
        this("[source error] " + e.getMessage(), e);
    }


    public CannotCompileException(ClassNotFoundException e, String name) {
        this("cannot find " + name, e);
    }


    public CannotCompileException(ClassFormatError e, String name) {
        this("invalid class format: " + name, e);
    }
}
