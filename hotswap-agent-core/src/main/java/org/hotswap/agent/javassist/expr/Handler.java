

package org.hotswap.agent.javassist.expr;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.CtBehavior;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.Bytecode;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.ExceptionTable;
import org.hotswap.agent.javassist.bytecode.MethodInfo;
import org.hotswap.agent.javassist.bytecode.Opcode;
import org.hotswap.agent.javassist.compiler.CompileError;
import org.hotswap.agent.javassist.compiler.Javac;


public class Handler extends Expr {
    private static String EXCEPTION_NAME = "$1";
    private ExceptionTable etable;
    private int index;


    protected Handler(ExceptionTable et, int nth,
                      CodeIterator it, CtClass declaring, MethodInfo m) {
        super(et.handlerPc(nth), it, declaring, m);
        etable = et;
        index = nth;
    }


    @Override
    public CtBehavior where() { return super.where(); }


    @Override
    public int getLineNumber() {
        return super.getLineNumber();
    }


    @Override
    public String getFileName() {
        return super.getFileName();
    }


    @Override
    public CtClass[] mayThrow() {
        return super.mayThrow();
    }


    public CtClass getType() throws NotFoundException {
        int type = etable.catchType(index);
        if (type == 0)
            return null;
        ConstPool cp = getConstPool();
        String name = cp.getClassInfo(type);
        return thisClass.getClassPool().getCtClass(name);
    }


    public boolean isFinally() {
        return etable.catchType(index) == 0;
    }


    @Override
    public void replace(String statement) throws CannotCompileException {
        throw new RuntimeException("not implemented yet");
    }


    public void insertBefore(String src) throws CannotCompileException {
        edited = true;

        @SuppressWarnings("unused")
        ConstPool cp = getConstPool();
        CodeAttribute ca = iterator.get();
        Javac jv = new Javac(thisClass);
        Bytecode b = jv.getBytecode();
        b.setStackDepth(1);
        b.setMaxLocals(ca.getMaxLocals());

        try {
            CtClass type = getType();
            int var = jv.recordVariable(type, EXCEPTION_NAME);
            jv.recordReturnType(type, false);
            b.addAstore(var);
            jv.compileStmnt(src);
            b.addAload(var);

            int oldHandler = etable.handlerPc(index);
            b.addOpcode(Opcode.GOTO);
            b.addIndex(oldHandler - iterator.getCodeLength()
                       - b.currentPc() + 1);

            maxStack = b.getMaxStack();
            maxLocals = b.getMaxLocals();

            int pos = iterator.append(b.get());
            iterator.append(b.getExceptionTable(), pos);
            etable.setHandlerPc(index, pos);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }
    }
}
