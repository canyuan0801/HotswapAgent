

package org.hotswap.agent.javassist;

import org.hotswap.agent.javassist.CtMethod.ConstParameter;
import org.hotswap.agent.javassist.bytecode.Bytecode;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.compiler.CompileError;
import org.hotswap.agent.javassist.compiler.Javac;


public class CtNewConstructor {

    public static final int PASS_NONE = 0;


    public static final int PASS_ARRAY = 1;


    public static final int PASS_PARAMS = 2;


    public static CtConstructor make(String src, CtClass declaring)
        throws CannotCompileException
    {
        Javac compiler = new Javac(declaring);
        try {
            CtMember obj = compiler.compile(src);
            if (obj instanceof CtConstructor) {

                return (CtConstructor)obj;
            }
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }

        throw new CannotCompileException("not a constructor");
    }


    public static CtConstructor make(CtClass[] parameters,
                                     CtClass[] exceptions,
                                     String body, CtClass declaring)
        throws CannotCompileException
    {
        try {
            CtConstructor cc = new CtConstructor(parameters, declaring);
            cc.setExceptionTypes(exceptions);
            cc.setBody(body);
            return cc;
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
    }


    public static CtConstructor copy(CtConstructor c, CtClass declaring,
                                ClassMap map) throws CannotCompileException {
        return new CtConstructor(c, declaring, map);
    }


    public static CtConstructor defaultConstructor(CtClass declaring)
        throws CannotCompileException
    {
        CtConstructor cons = new CtConstructor((CtClass[])null, declaring);

        ConstPool cp = declaring.getClassFile2().getConstPool();
        Bytecode code = new Bytecode(cp, 1, 1);
        code.addAload(0);
        try {
            code.addInvokespecial(declaring.getSuperclass(),
                                  "<init>", "()V");
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }

        code.add(Bytecode.RETURN);


        cons.getMethodInfo2().setCodeAttribute(code.toCodeAttribute());
        return cons;
    }


    public static CtConstructor skeleton(CtClass[] parameters,
                        CtClass[] exceptions, CtClass declaring)
        throws CannotCompileException
    {
        return make(parameters, exceptions, PASS_NONE,
                    null, null, declaring);
    }


    public static CtConstructor make(CtClass[] parameters,
                                     CtClass[] exceptions, CtClass declaring)
        throws CannotCompileException
    {
        return make(parameters, exceptions, PASS_PARAMS,
                    null, null, declaring);
    }


    public static CtConstructor make(CtClass[] parameters,
                                     CtClass[] exceptions, int howto,
                                     CtMethod body, ConstParameter cparam,
                                     CtClass declaring)
        throws CannotCompileException
    {
        return CtNewWrappedConstructor.wrapped(parameters, exceptions,
                                        howto, body, cparam, declaring);
    }
}
