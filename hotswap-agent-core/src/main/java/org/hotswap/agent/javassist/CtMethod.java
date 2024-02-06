

package org.hotswap.agent.javassist;

import org.hotswap.agent.javassist.bytecode.AccessFlag;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.Bytecode;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.Descriptor;
import org.hotswap.agent.javassist.bytecode.MethodInfo;
import org.hotswap.agent.javassist.bytecode.Opcode;


public final class CtMethod extends CtBehavior {
    protected String cachedStringRep;


    CtMethod(MethodInfo minfo, CtClass declaring) {
        super(declaring, minfo);
        cachedStringRep = null;
    }


    public CtMethod(CtClass returnType, String mname,
                    CtClass[] parameters, CtClass declaring) {
        this(null, declaring);
        ConstPool cp = declaring.getClassFile2().getConstPool();
        String desc = Descriptor.ofMethod(returnType, parameters);
        methodInfo = new MethodInfo(cp, mname, desc);
        setModifiers(Modifier.PUBLIC | Modifier.ABSTRACT);
    }


    public CtMethod(CtMethod src, CtClass declaring, ClassMap map)
        throws CannotCompileException
    {
        this(null, declaring);
        copy(src, false, map);
    }


    public static CtMethod make(String src, CtClass declaring)
        throws CannotCompileException
    {
        return CtNewMethod.make(src, declaring);
    }


    public static CtMethod make(MethodInfo minfo, CtClass declaring)
        throws CannotCompileException
    {
        if (declaring.getClassFile2().getConstPool() != minfo.getConstPool())
            throw new CannotCompileException("bad declaring class");

        return new CtMethod(minfo, declaring);
    }


    @Override
    public int hashCode() {
        return getStringRep().hashCode();
    }


    @Override
    void nameReplaced() {
        cachedStringRep = null;
    }


    final String getStringRep() {
        if (cachedStringRep == null)
            cachedStringRep = methodInfo.getName()
                + Descriptor.getParamDescriptor(methodInfo.getDescriptor());

        return cachedStringRep;
    }


    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof CtMethod
               && ((CtMethod)obj).getStringRep().equals(getStringRep());
    }


    @Override
    public String getLongName() {
        return getDeclaringClass().getName() + "."
               + getName() + Descriptor.toString(getSignature());
    }


    @Override
    public String getName() {
        return methodInfo.getName();
    }


    public void setName(String newname) {
        declaringClass.checkModify();
        methodInfo.setName(newname);
    }


    public CtClass getReturnType() throws NotFoundException {
        return getReturnType0();
    }


    @Override
    public boolean isEmpty() {
        CodeAttribute ca = getMethodInfo2().getCodeAttribute();
        if (ca == null)
            return (getModifiers() & Modifier.ABSTRACT) != 0;

        CodeIterator it = ca.iterator();
        try {
            return it.hasNext() && it.byteAt(it.next()) == Opcode.RETURN
                && !it.hasNext();
        }
        catch (BadBytecode e) {}
        return false;
    }


    public void setBody(CtMethod src, ClassMap map)
        throws CannotCompileException
    {
        setBody0(src.declaringClass, src.methodInfo,
                 declaringClass, methodInfo, map);
    }


    public void setWrappedBody(CtMethod mbody, ConstParameter constParam)
        throws CannotCompileException
    {
        declaringClass.checkModify();

        CtClass clazz = getDeclaringClass();
        CtClass[] params;
        CtClass retType;
        try {
            params = getParameterTypes();
            retType = getReturnType();
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }

        Bytecode code = CtNewWrappedMethod.makeBody(clazz,
                                                    clazz.getClassFile2(),
                                                    mbody,
                                                    params, retType,
                                                    constParam);
        CodeAttribute cattr = code.toCodeAttribute();
        methodInfo.setCodeAttribute(cattr);
        methodInfo.setAccessFlags(methodInfo.getAccessFlags()
                                  & ~AccessFlag.ABSTRACT);

    }




    public static class ConstParameter {

        public static ConstParameter integer(int i) {
            return new IntConstParameter(i);
        }


        public static ConstParameter integer(long i) {
            return new LongConstParameter(i);
        }


        public static ConstParameter string(String s) {
            return new StringConstParameter(s);
        }

        ConstParameter() {}


        int compile(Bytecode code) throws CannotCompileException {
            return 0;
        }

        String descriptor() {
            return defaultDescriptor();
        }


        static String defaultDescriptor() {
            return "([Ljava/lang/Object;)Ljava/lang/Object;";
        }


        String constDescriptor() {
            return defaultConstDescriptor();
        }


        static String defaultConstDescriptor() {
            return "([Ljava/lang/Object;)V";
        }
    }

    static class IntConstParameter extends ConstParameter {
        int param;

        IntConstParameter(int i) {
            param = i;
        }

        @Override
        int compile(Bytecode code) throws CannotCompileException {
            code.addIconst(param);
            return 1;
        }

        @Override
        String descriptor() {
            return "([Ljava/lang/Object;I)Ljava/lang/Object;";
        }

        @Override
        String constDescriptor() {
            return "([Ljava/lang/Object;I)V";
        }
    }

    static class LongConstParameter extends ConstParameter {
        long param;

        LongConstParameter(long l) {
            param = l;
        }

        @Override
        int compile(Bytecode code) throws CannotCompileException {
            code.addLconst(param);
            return 2;
        }

        @Override
        String descriptor() {
            return "([Ljava/lang/Object;J)Ljava/lang/Object;";
        }

        @Override
        String constDescriptor() {
            return "([Ljava/lang/Object;J)V";
        }
    }

    static class StringConstParameter extends ConstParameter {
        String param;

        StringConstParameter(String s) {
            param = s;
        }

        @Override
        int compile(Bytecode code) throws CannotCompileException {
            code.addLdc(param);
            return 1;
        }

        @Override
        String descriptor() {
            return "([Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;";
        }

        @Override
        String constDescriptor() {
            return "([Ljava/lang/Object;Ljava/lang/String;)V";
        }
    }
}
