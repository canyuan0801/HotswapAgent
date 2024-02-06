

package org.hotswap.agent.javassist.expr;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtBehavior;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.Bytecode;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.Descriptor;
import org.hotswap.agent.javassist.bytecode.MethodInfo;
import org.hotswap.agent.javassist.bytecode.Opcode;
import org.hotswap.agent.javassist.compiler.CompileError;
import org.hotswap.agent.javassist.compiler.Javac;
import org.hotswap.agent.javassist.compiler.JvstCodeGen;
import org.hotswap.agent.javassist.compiler.JvstTypeChecker;
import org.hotswap.agent.javassist.compiler.ProceedHandler;
import org.hotswap.agent.javassist.compiler.ast.ASTList;


public class NewExpr extends Expr {
    String newTypeName;
    int newPos;


    protected NewExpr(int pos, CodeIterator i, CtClass declaring,
                      MethodInfo m, String type, int np) {
        super(pos, i, declaring, m);
        newTypeName = type;
        newPos = np;
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


    private CtClass getCtClass() throws NotFoundException {
        return thisClass.getClassPool().get(newTypeName);
    }


    public String getClassName() {
        return newTypeName;
    }


    public String getSignature() {
        ConstPool constPool = getConstPool();
        int methodIndex = iterator.u16bitAt(currentPos + 1);
        return constPool.getMethodrefType(methodIndex);
    }


    public CtConstructor getConstructor() throws NotFoundException {
        ConstPool cp = getConstPool();
        int index = iterator.u16bitAt(currentPos + 1);
        String desc = cp.getMethodrefType(index);
        return getCtClass().getConstructor(desc);
    }


    @Override
    public CtClass[] mayThrow() {
        return super.mayThrow();
    }



    private int canReplace() throws CannotCompileException {
        int op = iterator.byteAt(newPos + 3);
        if (op == Opcode.DUP)
            return ((iterator.byteAt(newPos + 4) == Opcode.DUP2_X2
                 && iterator.byteAt(newPos + 5) == Opcode.POP2)) ? 6 : 4;
        else if (op == Opcode.DUP_X1
                 && iterator.byteAt(newPos + 4) == Opcode.SWAP)
            return 5;
        else
            return 3;


    }


    @Override
    public void replace(String statement) throws CannotCompileException {
        thisClass.getClassFile();

        final int bytecodeSize = 3;
        int pos = newPos;

        int newIndex = iterator.u16bitAt(pos + 1);


        int codeSize = canReplace();
        int end = pos + codeSize;
        for (int i = pos; i < end; ++i)
            iterator.writeByte(NOP, i);

        ConstPool constPool = getConstPool();
        pos = currentPos;
        int methodIndex = iterator.u16bitAt(pos + 1);

        String signature = constPool.getMethodrefType(methodIndex);

        Javac jc = new Javac(thisClass);
        ClassPool cp = thisClass.getClassPool();
        CodeAttribute ca = iterator.get();
        try {
            CtClass[] params = Descriptor.getParameterTypes(signature, cp);
            CtClass newType = cp.get(newTypeName);
            int paramVar = ca.getMaxLocals();
            jc.recordParams(newTypeName, params,
                            true, paramVar, withinStatic());
            int retVar = jc.recordReturnType(newType, true);
            jc.recordProceed(new ProceedForNew(newType, newIndex,
                                               methodIndex));


            checkResultValue(newType, statement);

            Bytecode bytecode = jc.getBytecode();
            storeStack(params, true, paramVar, bytecode);
            jc.recordLocalVariables(ca, pos);

            bytecode.addConstZero(newType);
            bytecode.addStore(retVar, newType);

            jc.compileStmnt(statement);
            if (codeSize > 3)
                bytecode.addAload(retVar);

            replace0(pos, bytecode, bytecodeSize);
        }
        catch (CompileError e) { throw new CannotCompileException(e); }
        catch (NotFoundException e) { throw new CannotCompileException(e); }
        catch (BadBytecode e) {
            throw new CannotCompileException("broken method");
        }
    }

    static class ProceedForNew implements ProceedHandler {
        CtClass newType;
        int newIndex, methodIndex;

        ProceedForNew(CtClass nt, int ni, int mi) {
            newType = nt;
            newIndex = ni;
            methodIndex = mi;
        }

        @Override
        public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args)
            throws CompileError
        {
            bytecode.addOpcode(NEW);
            bytecode.addIndex(newIndex);
            bytecode.addOpcode(DUP);
            gen.atMethodCallCore(newType, MethodInfo.nameInit, args,
                                 false, true, -1, null);
            gen.setType(newType);
        }

        @Override
        public void setReturnType(JvstTypeChecker c, ASTList args)
            throws CompileError
        {
            c.atMethodCallCore(newType, MethodInfo.nameInit, args);
            c.setType(newType);
        }
    }
}
