

package org.hotswap.agent.javassist.expr;

import java.util.LinkedList;
import java.util.List;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtBehavior;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.CtPrimitiveType;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.AccessFlag;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.Bytecode;
import org.hotswap.agent.javassist.bytecode.ClassFile;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.ExceptionTable;
import org.hotswap.agent.javassist.bytecode.ExceptionsAttribute;
import org.hotswap.agent.javassist.bytecode.MethodInfo;
import org.hotswap.agent.javassist.bytecode.Opcode;
import org.hotswap.agent.javassist.compiler.Javac;


public abstract class Expr implements Opcode {
    int currentPos;
    CodeIterator iterator;
    CtClass thisClass;
    MethodInfo thisMethod;
    boolean edited;
    int maxLocals, maxStack;

    static final String javaLangObject = "java.lang.Object";


    protected Expr(int pos, CodeIterator i, CtClass declaring, MethodInfo m) {
        currentPos = pos;
        iterator = i;
        thisClass = declaring;
        thisMethod = m;
    }


    public CtClass getEnclosingClass() { return thisClass; }

    protected final ConstPool getConstPool() {
        return thisMethod.getConstPool();
    }

    protected final boolean edited() {
        return edited;
    }

    protected final int locals() {
        return maxLocals;
    }

    protected final int stack() {
        return maxStack;
    }


    protected final boolean withinStatic() {
        return (thisMethod.getAccessFlags() & AccessFlag.STATIC) != 0;
    }


    public CtBehavior where() {
        MethodInfo mi = thisMethod;
        CtBehavior[] cb = thisClass.getDeclaredBehaviors();
        for (int i = cb.length - 1; i >= 0; --i)
            if (cb[i].getMethodInfo2() == mi)
                return cb[i];

        CtConstructor init = thisClass.getClassInitializer();
        if (init != null && init.getMethodInfo2() == mi)
            return init;


        for (int i = cb.length - 1; i >= 0; --i) {
            if (thisMethod.getName().equals(cb[i].getMethodInfo2().getName())
                && thisMethod.getDescriptor()
                             .equals(cb[i].getMethodInfo2().getDescriptor())) {
                return cb[i];
            }
        }

        throw new RuntimeException("fatal: not found");
    }


    public CtClass[] mayThrow() {
        ClassPool pool = thisClass.getClassPool();
        ConstPool cp = thisMethod.getConstPool();
        List<CtClass> list = new LinkedList<CtClass>();
        try {
            CodeAttribute ca = thisMethod.getCodeAttribute();
            ExceptionTable et = ca.getExceptionTable();
            int pos = currentPos;
            int n = et.size();
            for (int i = 0; i < n; ++i)
                if (et.startPc(i) <= pos && pos < et.endPc(i)) {
                    int t = et.catchType(i);
                    if (t > 0)
                        try {
                            addClass(list, pool.get(cp.getClassInfo(t)));
                        }
                        catch (NotFoundException e) {
                        }
                }
        }
        catch (NullPointerException e) {
        }

        ExceptionsAttribute ea = thisMethod.getExceptionsAttribute();
        if (ea != null) {
            String[] exceptions = ea.getExceptions();
            if (exceptions != null) {
                int n = exceptions.length;
                for (int i = 0; i < n; ++i)
                    try {
                        addClass(list, pool.get(exceptions[i]));
                    }
                    catch (NotFoundException e) {
                    }
            }
        }

        return list.toArray(new CtClass[list.size()]);
    }

    private static void addClass(List<CtClass> list, CtClass c) {
        if (list.contains(c))
            return;

        list.add(c);
    }


    public int indexOfBytecode() {
        return currentPos;
    }


    public int getLineNumber() {
        return thisMethod.getLineNumber(currentPos);
    }


    public String getFileName() {
        ClassFile cf = thisClass.getClassFile2();
        if (cf == null)
            return null;
        return cf.getSourceFile();
    }

    static final boolean checkResultValue(CtClass retType, String prog)
            throws CannotCompileException {

        boolean hasIt = (prog.indexOf(Javac.resultVarName) >= 0);
        if (!hasIt && retType != CtClass.voidType)
            throw new CannotCompileException(
                    "the resulting value is not stored in "
                            + Javac.resultVarName);

        return hasIt;
    }


    static final void storeStack(CtClass[] params, boolean isStaticCall,
            int regno, Bytecode bytecode) {
        storeStack0(0, params.length, params, regno + 1, bytecode);
        if (isStaticCall)
            bytecode.addOpcode(ACONST_NULL);

        bytecode.addAstore(regno);
    }

    private static void storeStack0(int i, int n, CtClass[] params, int regno,
            Bytecode bytecode) {
        if (i >= n)
            return;
        CtClass c = params[i];
        int size;
        if (c instanceof CtPrimitiveType)
            size = ((CtPrimitiveType)c).getDataSize();
        else
            size = 1;

        storeStack0(i + 1, n, params, regno + size, bytecode);
        bytecode.addStore(regno, c);
    }







    public abstract void replace(String statement) throws CannotCompileException;


    public void replace(String statement, ExprEditor recursive)
        throws CannotCompileException
    {
        replace(statement);
        if (recursive != null)
            runEditor(recursive, iterator);
    }

    protected void replace0(int pos, Bytecode bytecode, int size)
            throws BadBytecode {
        byte[] code = bytecode.get();
        edited = true;
        int gap = code.length - size;
        for (int i = 0; i < size; ++i)
            iterator.writeByte(NOP, pos + i);

        if (gap > 0)
            pos = iterator.insertGapAt(pos, gap, false).position;

        iterator.write(code, pos);
        iterator.insert(bytecode.getExceptionTable(), pos);
        maxLocals = bytecode.getMaxLocals();
        maxStack = bytecode.getMaxStack();
    }

    protected void runEditor(ExprEditor ed, CodeIterator oldIterator)
        throws CannotCompileException
    {
        CodeAttribute codeAttr = oldIterator.get();
        int orgLocals = codeAttr.getMaxLocals();
        int orgStack = codeAttr.getMaxStack();
        int newLocals = locals();
        codeAttr.setMaxStack(stack());
        codeAttr.setMaxLocals(newLocals);
        ExprEditor.LoopContext context
            = new ExprEditor.LoopContext(newLocals);
        int size = oldIterator.getCodeLength();
        int endPos = oldIterator.lookAhead();
        oldIterator.move(currentPos);
        if (ed.doit(thisClass, thisMethod, context, oldIterator, endPos))
            edited = true;

        oldIterator.move(endPos + oldIterator.getCodeLength() - size);
        codeAttr.setMaxLocals(orgLocals);
        codeAttr.setMaxStack(orgStack);
        maxLocals = context.maxLocals;
        maxStack += context.maxStack;
    }
}
