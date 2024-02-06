

package org.hotswap.agent.javassist.expr;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtBehavior;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.Bytecode;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.MethodInfo;
import org.hotswap.agent.javassist.bytecode.Opcode;
import org.hotswap.agent.javassist.compiler.CompileError;
import org.hotswap.agent.javassist.compiler.Javac;
import org.hotswap.agent.javassist.compiler.JvstCodeGen;
import org.hotswap.agent.javassist.compiler.JvstTypeChecker;
import org.hotswap.agent.javassist.compiler.ProceedHandler;
import org.hotswap.agent.javassist.compiler.ast.ASTList;


public class Cast extends Expr {

    protected Cast(int pos, CodeIterator i, CtClass declaring, MethodInfo m) {
        super(pos, i, declaring, m);
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


    public CtClass getType() throws NotFoundException {
        ConstPool cp = getConstPool();
        int pos = currentPos;
        int index = iterator.u16bitAt(pos + 1);
        String name = cp.getClassInfo(index);
        return thisClass.getClassPool().getCtClass(name);
    }


    @Override
    public CtClass[] mayThrow() {
        return super.mayThrow();
    }


    @Override
    public void replace(String statement) throws CannotCompileException {
        thisClass.getClassFile();
        @SuppressWarnings("unused")
        ConstPool constPool = getConstPool();
        int pos = currentPos;
        int index = iterator.u16bitAt(pos + 1);

        Javac jc = new Javac(thisClass);
        ClassPool cp = thisClass.getClassPool();
        CodeAttribute ca = iterator.get();

        try {
            CtClass[] params
                = new CtClass[] { cp.get(javaLangObject) };
            CtClass retType = getType();

            int paramVar = ca.getMaxLocals();
            jc.recordParams(javaLangObject, params, true, paramVar,
                            withinStatic());
            int retVar = jc.recordReturnType(retType, true);
            jc.recordProceed(new ProceedForCast(index, retType));


            checkResultValue(retType, statement);

            Bytecode bytecode = jc.getBytecode();
            storeStack(params, true, paramVar, bytecode);
            jc.recordLocalVariables(ca, pos);

            bytecode.addConstZero(retType);
            bytecode.addStore(retVar, retType);

            jc.compileStmnt(statement);
            bytecode.addLoad(retVar, retType);

            replace0(pos, bytecode, 3);
        }
        catch (CompileError e) { throw new CannotCompileException(e); }
        catch (NotFoundException e) { throw new CannotCompileException(e); }
        catch (BadBytecode e) {
            throw new CannotCompileException("broken method");
        }
    }


    static class ProceedForCast implements ProceedHandler {
        int index;
        CtClass retType;

        ProceedForCast(int i, CtClass t) {
            index = i;
            retType = t;
        }

        @Override
        public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args)
            throws CompileError
        {
            if (gen.getMethodArgsLength(args) != 1)
                throw new CompileError(Javac.proceedName
                        + "() cannot take more than one parameter "
                        + "for cast");

            gen.atMethodArgs(args, new int[1], new int[1], new String[1]);
            bytecode.addOpcode(Opcode.CHECKCAST);
            bytecode.addIndex(index);
            gen.setType(retType);
        }

        @Override
        public void setReturnType(JvstTypeChecker c, ASTList args)
            throws CompileError
        {
            c.atMethodArgs(args, new int[1], new int[1], new String[1]);
            c.setType(retType);
        }
    }
}
