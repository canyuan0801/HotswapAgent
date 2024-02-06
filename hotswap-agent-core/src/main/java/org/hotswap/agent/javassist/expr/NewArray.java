

package org.hotswap.agent.javassist.expr;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.CtBehavior;
import org.hotswap.agent.javassist.CtClass;
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


public class NewArray extends Expr {
    int opcode;

    protected NewArray(int pos, CodeIterator i, CtClass declaring,
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


    @Override
    public CtClass[] mayThrow() {
        return super.mayThrow();
    }


    public CtClass getComponentType() throws NotFoundException {
        if (opcode == Opcode.NEWARRAY) {
            int atype = iterator.byteAt(currentPos + 1);
            return getPrimitiveType(atype);
        }
        else if (opcode == Opcode.ANEWARRAY
                 || opcode == Opcode.MULTIANEWARRAY) {
            int index = iterator.u16bitAt(currentPos + 1);
            String desc = getConstPool().getClassInfo(index);
            int dim = Descriptor.arrayDimension(desc);
            desc = Descriptor.toArrayComponent(desc, dim);
            return Descriptor.toCtClass(desc, thisClass.getClassPool());
        }
        else
            throw new RuntimeException("bad opcode: " + opcode);
    }

    CtClass getPrimitiveType(int atype) {
        switch (atype) {
        case Opcode.T_BOOLEAN :
            return CtClass.booleanType;
        case Opcode.T_CHAR :
            return CtClass.charType;
        case Opcode.T_FLOAT :
            return CtClass.floatType;
        case Opcode.T_DOUBLE :
            return CtClass.doubleType;
        case Opcode.T_BYTE :
            return CtClass.byteType;
        case Opcode.T_SHORT :
            return CtClass.shortType;
        case Opcode.T_INT :
            return CtClass.intType;
        case Opcode.T_LONG :
            return CtClass.longType;
        default :
            throw new RuntimeException("bad atype: " + atype);        
        }
    }


    public int getDimension() {
        if (opcode == Opcode.NEWARRAY)
            return 1;
        else if (opcode == Opcode.ANEWARRAY
                 || opcode == Opcode.MULTIANEWARRAY) {
            int index = iterator.u16bitAt(currentPos + 1);
            String desc = getConstPool().getClassInfo(index);
            return Descriptor.arrayDimension(desc)
                    + (opcode == Opcode.ANEWARRAY ? 1 : 0);
        }
        else
            throw new RuntimeException("bad opcode: " + opcode);
    }


    public int getCreatedDimensions() {
        if (opcode == Opcode.MULTIANEWARRAY)
            return iterator.byteAt(currentPos + 3);
        return 1;
    }


    @Override
    public void replace(String statement) throws CannotCompileException {
        try {
            replace2(statement);
        }
        catch (CompileError e) { throw new CannotCompileException(e); }
        catch (NotFoundException e) { throw new CannotCompileException(e); }
        catch (BadBytecode e) {
            throw new CannotCompileException("broken method");
        }
    }

    private void replace2(String statement)
        throws CompileError, NotFoundException, BadBytecode,
               CannotCompileException
    {
        thisClass.getClassFile();
        ConstPool constPool = getConstPool();
        int pos = currentPos;
        CtClass retType;
        int codeLength;
        int index = 0;
        int dim = 1;
        String desc;
        if (opcode == Opcode.NEWARRAY) {
            index = iterator.byteAt(currentPos + 1);
            CtPrimitiveType cpt = (CtPrimitiveType)getPrimitiveType(index); 
            desc = "[" + cpt.getDescriptor();
            codeLength = 2;
        }
        else if (opcode == Opcode.ANEWARRAY) {
            index = iterator.u16bitAt(pos + 1);
            desc = constPool.getClassInfo(index);
            if (desc.startsWith("["))
                desc = "[" + desc;
            else
                desc = "[L" + desc + ";";

            codeLength = 3;
        }
        else if (opcode == Opcode.MULTIANEWARRAY) {
            index = iterator.u16bitAt(currentPos + 1);
            desc = constPool.getClassInfo(index);
            dim = iterator.byteAt(currentPos + 3);
            codeLength = 4;
        }
        else
            throw new RuntimeException("bad opcode: " + opcode);

        retType = Descriptor.toCtClass(desc, thisClass.getClassPool());

        Javac jc = new Javac(thisClass);
        CodeAttribute ca = iterator.get();

        CtClass[] params = new CtClass[dim];
        for (int i = 0; i < dim; ++i)
            params[i] = CtClass.intType;

        int paramVar = ca.getMaxLocals();
        jc.recordParams(javaLangObject, params,
                        true, paramVar, withinStatic());


        checkResultValue(retType, statement);
        int retVar = jc.recordReturnType(retType, true);
        jc.recordProceed(new ProceedForArray(retType, opcode, index, dim));

        Bytecode bytecode = jc.getBytecode();
        storeStack(params, true, paramVar, bytecode);
        jc.recordLocalVariables(ca, pos);

        bytecode.addOpcode(ACONST_NULL);
        bytecode.addAstore(retVar);

        jc.compileStmnt(statement);
        bytecode.addAload(retVar);

        replace0(pos, bytecode, codeLength);
    }


    static class ProceedForArray implements ProceedHandler {
        CtClass arrayType;
        int opcode;
        int index, dimension;

        ProceedForArray(CtClass type, int op, int i, int dim) {
            arrayType = type;
            opcode = op;
            index = i;
            dimension = dim;
        }

        @Override
        public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args)
            throws CompileError
        {
            int num = gen.getMethodArgsLength(args);
            if (num != dimension)
                throw new CompileError(Javac.proceedName
                        + "() with a wrong number of parameters");

            gen.atMethodArgs(args, new int[num],
                             new int[num], new String[num]);
            bytecode.addOpcode(opcode);
            if (opcode == Opcode.ANEWARRAY)
                bytecode.addIndex(index);
            else if (opcode == Opcode.NEWARRAY)
                bytecode.add(index);
            else  {
                bytecode.addIndex(index);
                bytecode.add(dimension);
                bytecode.growStack(1 - dimension);
            }

            gen.setType(arrayType);
        }

        @Override
        public void setReturnType(JvstTypeChecker c, ASTList args)
            throws CompileError
        {
            c.setType(arrayType);
        }
    }
}
