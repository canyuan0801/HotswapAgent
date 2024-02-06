

package org.hotswap.agent.javassist.expr;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtBehavior;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.Bytecode;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.Descriptor;
import org.hotswap.agent.javassist.bytecode.MethodInfo;
import org.hotswap.agent.javassist.compiler.CompileError;
import org.hotswap.agent.javassist.compiler.Javac;


public class MethodCall extends Expr {
    
    protected MethodCall(int pos, CodeIterator i, CtClass declaring,
                         MethodInfo m) {
        super(pos, i, declaring, m);
    }

    private int getNameAndType(ConstPool cp) {
        int pos = currentPos;
        int c = iterator.byteAt(pos);
        int index = iterator.u16bitAt(pos + 1);

        if (c == INVOKEINTERFACE)
            return cp.getInterfaceMethodrefNameAndType(index);
        return cp.getMethodrefNameAndType(index);
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

    
    protected CtClass getCtClass() throws NotFoundException {
        return thisClass.getClassPool().get(getClassName());
    }

    
    public String getClassName() {
        String cname;

        ConstPool cp = getConstPool();
        int pos = currentPos;
        int c = iterator.byteAt(pos);
        int index = iterator.u16bitAt(pos + 1);

        if (c == INVOKEINTERFACE)
            cname = cp.getInterfaceMethodrefClassName(index);
        else
            cname = cp.getMethodrefClassName(index);

         if (cname.charAt(0) == '[')
             cname = Descriptor.toClassName(cname);

         return cname;
    }

    
    public String getMethodName() {
        ConstPool cp = getConstPool();
        int nt = getNameAndType(cp);
        return cp.getUtf8Info(cp.getNameAndTypeName(nt));
    }

    
    public CtMethod getMethod() throws NotFoundException {
        return getCtClass().getMethod(getMethodName(), getSignature());
    }

    
    public String getSignature() {
        ConstPool cp = getConstPool();
        int nt = getNameAndType(cp);
        return cp.getUtf8Info(cp.getNameAndTypeDescriptor(nt));
    }

    
    @Override
    public CtClass[] mayThrow() {
        return super.mayThrow();
    }

    
    public boolean isSuper() {
        return iterator.byteAt(currentPos) == INVOKESPECIAL
            && !where().getDeclaringClass().getName().equals(getClassName());
    }

    

    

    
    @Override
    public void replace(String statement) throws CannotCompileException {
        thisClass.getClassFile();   
        ConstPool constPool = getConstPool();
        int pos = currentPos;
        int index = iterator.u16bitAt(pos + 1);

        String classname, methodname, signature;
        int opcodeSize;
        int c = iterator.byteAt(pos);
        if (c == INVOKEINTERFACE) {
            opcodeSize = 5;
            classname = constPool.getInterfaceMethodrefClassName(index);
            methodname = constPool.getInterfaceMethodrefName(index);
            signature = constPool.getInterfaceMethodrefType(index);
        }
        else if (c == INVOKESTATIC
                 || c == INVOKESPECIAL || c == INVOKEVIRTUAL) {
            opcodeSize = 3;
            classname = constPool.getMethodrefClassName(index);
            methodname = constPool.getMethodrefName(index);
            signature = constPool.getMethodrefType(index);
        }
        else
            throw new CannotCompileException("not method invocation");

        Javac jc = new Javac(thisClass);
        ClassPool cp = thisClass.getClassPool();
        CodeAttribute ca = iterator.get();
        try {
            CtClass[] params = Descriptor.getParameterTypes(signature, cp);
            CtClass retType = Descriptor.getReturnType(signature, cp);
            int paramVar = ca.getMaxLocals();
            jc.recordParams(classname, params,
                            true, paramVar, withinStatic());
            int retVar = jc.recordReturnType(retType, true);
            if (c == INVOKESTATIC)
                jc.recordStaticProceed(classname, methodname);
            else if (c == INVOKESPECIAL)
                jc.recordSpecialProceed(Javac.param0Name, classname,
                                        methodname, signature, index);
            else
                jc.recordProceed(Javac.param0Name, methodname);

            
            checkResultValue(retType, statement);

            Bytecode bytecode = jc.getBytecode();
            storeStack(params, c == INVOKESTATIC, paramVar, bytecode);
            jc.recordLocalVariables(ca, pos);

            if (retType != CtClass.voidType) {
                bytecode.addConstZero(retType);
                bytecode.addStore(retVar, retType);     
            }

            jc.compileStmnt(statement);
            if (retType != CtClass.voidType)
                bytecode.addLoad(retVar, retType);

            replace0(pos, bytecode, opcodeSize);
        }
        catch (CompileError e) { throw new CannotCompileException(e); }
        catch (NotFoundException e) { throw new CannotCompileException(e); }
        catch (BadBytecode e) {
            throw new CannotCompileException("broken method");
        }
    }
}
