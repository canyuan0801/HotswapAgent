

package org.hotswap.agent.javassist.expr;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtBehavior;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtField;
import org.hotswap.agent.javassist.CtPrimitiveType;
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


public class FieldAccess extends Expr {
    int opcode;

    protected FieldAccess(int pos, CodeIterator i, CtClass declaring,
                          MethodInfo m, int op) {
        super(pos, i, declaring, m);
        opcode = op;
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


    public boolean isStatic() {
        return isStatic(opcode);
    }

    static boolean isStatic(int c) {
        return c == Opcode.GETSTATIC || c == Opcode.PUTSTATIC;
    }


    public boolean isReader() {
        return opcode == Opcode.GETFIELD || opcode ==  Opcode.GETSTATIC;
    }


    public boolean isWriter() {
        return opcode == Opcode.PUTFIELD || opcode ==  Opcode.PUTSTATIC;
    }


    private CtClass getCtClass() throws NotFoundException {
        return thisClass.getClassPool().get(getClassName());
    }


    public String getClassName() {
        int index = iterator.u16bitAt(currentPos + 1);
        return getConstPool().getFieldrefClassName(index);
    }


    public String getFieldName() {
        int index = iterator.u16bitAt(currentPos + 1);
        return getConstPool().getFieldrefName(index);
    }


    public CtField getField() throws NotFoundException {
        CtClass cc = getCtClass();
        int index = iterator.u16bitAt(currentPos + 1);
        ConstPool cp = getConstPool();
        return cc.getField(cp.getFieldrefName(index), cp.getFieldrefType(index));
    }


    @Override
    public CtClass[] mayThrow() {
        return super.mayThrow();
    }


    public String getSignature() {
        int index = iterator.u16bitAt(currentPos + 1);
        return getConstPool().getFieldrefType(index);
    }


    @Override
    public void replace(String statement) throws CannotCompileException {
        thisClass.getClassFile();
        ConstPool constPool = getConstPool();
        int pos = currentPos;
        int index = iterator.u16bitAt(pos + 1);

        Javac jc = new Javac(thisClass);
        CodeAttribute ca = iterator.get();
        try {
            CtClass[] params;
            CtClass retType;
            CtClass fieldType
                = Descriptor.toCtClass(constPool.getFieldrefType(index),
                                       thisClass.getClassPool());
            boolean read = isReader();
            if (read) {
                params = new CtClass[0];
                retType = fieldType;
            }
            else {
                params = new CtClass[1];
                params[0] = fieldType;
                retType = CtClass.voidType;
            }

            int paramVar = ca.getMaxLocals();
            jc.recordParams(constPool.getFieldrefClassName(index), params,
                            true, paramVar, withinStatic());


            boolean included = checkResultValue(retType, statement);
            if (read)
                included = true;

            int retVar = jc.recordReturnType(retType, included);
            if (read)
                jc.recordProceed(new ProceedForRead(retType, opcode,
                                                    index, paramVar));
            else {

                jc.recordType(fieldType);
                jc.recordProceed(new ProceedForWrite(params[0], opcode,
                                                     index, paramVar));
            }

            Bytecode bytecode = jc.getBytecode();
            storeStack(params, isStatic(), paramVar, bytecode);
            jc.recordLocalVariables(ca, pos);

            if (included)
                if (retType == CtClass.voidType) {
                    bytecode.addOpcode(ACONST_NULL);
                    bytecode.addAstore(retVar);
                }
                else {
                    bytecode.addConstZero(retType);
                    bytecode.addStore(retVar, retType);
                }

            jc.compileStmnt(statement);
            if (read)
                bytecode.addLoad(retVar, retType);

            replace0(pos, bytecode, 3);
        }
        catch (CompileError e) { throw new CannotCompileException(e); }
        catch (NotFoundException e) { throw new CannotCompileException(e); }
        catch (BadBytecode e) {
            throw new CannotCompileException("broken method");
        }
    }


    static class ProceedForRead implements ProceedHandler {
        CtClass fieldType;
        int opcode;
        int targetVar, index;

        ProceedForRead(CtClass type, int op, int i, int var) {
            fieldType = type;
            targetVar = var;
            opcode = op;
            index = i;
        }

        @Override
        public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args)
            throws CompileError
        {
            if (args != null && !gen.isParamListName(args))
                throw new CompileError(Javac.proceedName
                        + "() cannot take a parameter for field reading");

            int stack;
            if (isStatic(opcode))
                stack = 0;
            else {
                stack = -1;
                bytecode.addAload(targetVar);
            }

            if (fieldType instanceof CtPrimitiveType)
                stack += ((CtPrimitiveType)fieldType).getDataSize();
            else
                ++stack;

            bytecode.add(opcode);
            bytecode.addIndex(index);
            bytecode.growStack(stack);
            gen.setType(fieldType);
        }

        @Override
        public void setReturnType(JvstTypeChecker c, ASTList args)
            throws CompileError
        {
            c.setType(fieldType);
        }
    }


    static class ProceedForWrite implements ProceedHandler {
        CtClass fieldType;
        int opcode;
        int targetVar, index;

        ProceedForWrite(CtClass type, int op, int i, int var) {
            fieldType = type;
            targetVar = var;
            opcode = op;
            index = i;
        }

        @Override
        public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args)
            throws CompileError
        {
            if (gen.getMethodArgsLength(args) != 1)
                throw new CompileError(Javac.proceedName
                        + "() cannot take more than one parameter "
                        + "for field writing");

            int stack;
            if (isStatic(opcode))
                stack = 0;
            else {
                stack = -1;
                bytecode.addAload(targetVar);
            }

            gen.atMethodArgs(args, new int[1], new int[1], new String[1]);
            gen.doNumCast(fieldType);
            if (fieldType instanceof CtPrimitiveType)
                stack -= ((CtPrimitiveType)fieldType).getDataSize();
            else
                --stack;

            bytecode.add(opcode);
            bytecode.addIndex(index);
            bytecode.growStack(stack);
            gen.setType(CtClass.voidType);
            gen.addNullIfVoid();
        }

        @Override
        public void setReturnType(JvstTypeChecker c, ASTList args)
            throws CompileError
        {
            c.atMethodArgs(args, new int[1], new int[1], new String[1]);
            c.setType(CtClass.voidType);
            c.addNullIfVoid();
        }
    }
}
