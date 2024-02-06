

package org.hotswap.agent.javassist;

import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.Bytecode;
import org.hotswap.agent.javassist.bytecode.ClassFile;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.Descriptor;
import org.hotswap.agent.javassist.bytecode.MethodInfo;
import org.hotswap.agent.javassist.bytecode.Opcode;
import org.hotswap.agent.javassist.compiler.CompileError;
import org.hotswap.agent.javassist.compiler.Javac;


public final class CtConstructor extends CtBehavior {
    protected CtConstructor(MethodInfo minfo, CtClass declaring) {
        super(declaring, minfo);
    }


    public CtConstructor(CtClass[] parameters, CtClass declaring) {
        this((MethodInfo)null, declaring);
        ConstPool cp = declaring.getClassFile2().getConstPool();
        String desc = Descriptor.ofConstructor(parameters);
        methodInfo = new MethodInfo(cp, "<init>", desc);
        setModifiers(Modifier.PUBLIC);
    }


    public CtConstructor(CtConstructor src, CtClass declaring, ClassMap map)
        throws CannotCompileException
    {
        this((MethodInfo)null, declaring);
        copy(src, true, map);
    }


    public boolean isConstructor() {
        return methodInfo.isConstructor();
    }


    public boolean isClassInitializer() {
        return methodInfo.isStaticInitializer();
    }


    @Override
    public String getLongName() {
        return getDeclaringClass().getName()
               + (isConstructor() ? Descriptor.toString(getSignature())
                                  : ("." + MethodInfo.nameClinit + "()"));
    }


    @Override
    public String getName() {
        if (methodInfo.isStaticInitializer())
            return MethodInfo.nameClinit;
        return declaringClass.getSimpleName();
    }


    @Override
    public boolean isEmpty() {
        CodeAttribute ca = getMethodInfo2().getCodeAttribute();
        if (ca == null)
            return false;


        ConstPool cp = ca.getConstPool();
        CodeIterator it = ca.iterator();
        try {
            int pos, desc;
            int op0 = it.byteAt(it.next());
            return op0 == Opcode.RETURN
                || (op0 == Opcode.ALOAD_0
                    && it.byteAt(pos = it.next()) == Opcode.INVOKESPECIAL
                    && (desc = cp.isConstructor(getSuperclassName(),
                                                it.u16bitAt(pos + 1))) != 0
                    && "()V".equals(cp.getUtf8Info(desc))
                    && it.byteAt(it.next()) == Opcode.RETURN
                    && !it.hasNext());
        }
        catch (BadBytecode e) {}
        return false;
    }

    private String getSuperclassName() {
        ClassFile cf = declaringClass.getClassFile2();
        return cf.getSuperclass();
    }


    public boolean callsSuper() throws CannotCompileException {
        CodeAttribute codeAttr = methodInfo.getCodeAttribute();
        if (codeAttr != null) {
            CodeIterator it = codeAttr.iterator();
            try {
                int index = it.skipSuperConstructor();
                return index >= 0;
            }
            catch (BadBytecode e) {
                throw new CannotCompileException(e);
            }
        }

        return false;
    }


    @Override
    public void setBody(String src) throws CannotCompileException {
        if (src == null)
            if (isClassInitializer())
                src = ";";
            else
                src = "super();";

        super.setBody(src);
    }


    public void setBody(CtConstructor src, ClassMap map)
        throws CannotCompileException
    {
        setBody0(src.declaringClass, src.methodInfo,
                 declaringClass, methodInfo, map);
    }


    public void insertBeforeBody(String src) throws CannotCompileException {
        CtClass cc = declaringClass;
        cc.checkModify();
        if (isClassInitializer())
            throw new CannotCompileException("class initializer");

        CodeAttribute ca = methodInfo.getCodeAttribute();
        CodeIterator iterator = ca.iterator();
        Bytecode b = new Bytecode(methodInfo.getConstPool(),
                                  ca.getMaxStack(), ca.getMaxLocals());
        b.setStackDepth(ca.getMaxStack());
        Javac jv = new Javac(b, cc);
        try {
            jv.recordParams(getParameterTypes(), false);
            jv.compileStmnt(src);
            ca.setMaxStack(b.getMaxStack());
            ca.setMaxLocals(b.getMaxLocals());
            iterator.skipConstructor();
            int pos = iterator.insertEx(b.get());
            iterator.insert(b.getExceptionTable(), pos);
            methodInfo.rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile2());
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }


    @Override
    int getStartPosOfBody(CodeAttribute ca) throws CannotCompileException {
        CodeIterator ci = ca.iterator();
        try {
            ci.skipConstructor();
            return ci.next();
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }


    public CtMethod toMethod(String name, CtClass declaring)
        throws CannotCompileException
    {
        return toMethod(name, declaring, null);
    }


    public CtMethod toMethod(String name, CtClass declaring, ClassMap map)
        throws CannotCompileException
    {
        CtMethod method = new CtMethod(null, declaring);
        method.copy(this, false, map);
        if (isConstructor()) {
            MethodInfo minfo = method.getMethodInfo2();
            CodeAttribute ca = minfo.getCodeAttribute();
            if (ca != null) {
                removeConsCall(ca);
                try {
                    methodInfo.rebuildStackMapIf6(declaring.getClassPool(),
                                                  declaring.getClassFile2());
                }
                catch (BadBytecode e) {
                    throw new CannotCompileException(e);
                }
            }
        }

        method.setName(name);
        return method;
    }

    private static void removeConsCall(CodeAttribute ca)
        throws CannotCompileException
    {
        CodeIterator iterator = ca.iterator();
        try {
            int pos = iterator.skipConstructor();
            if (pos >= 0) {
                int mref = iterator.u16bitAt(pos + 1);
                String desc = ca.getConstPool().getMethodrefType(mref);
                int num = Descriptor.numOfParameters(desc) + 1;
                if (num > 3)
                    pos = iterator.insertGapAt(pos, num - 3, false).position;

                iterator.writeByte(Opcode.POP, pos++);
                iterator.writeByte(Opcode.NOP, pos);
                iterator.writeByte(Opcode.NOP, pos + 1);
                Descriptor.Iterator it = new Descriptor.Iterator(desc);
                while (true) {
                    it.next();
                    if (it.isParameter())
                        iterator.writeByte(it.is2byte() ? Opcode.POP2 : Opcode.POP,
                                           pos++);
                    else
                        break;
                }
            }
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }
}
