

package org.hotswap.agent.javassist;

import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.MethodInfo;
import org.hotswap.agent.javassist.convert.TransformAccessArrayField;
import org.hotswap.agent.javassist.convert.TransformAfter;
import org.hotswap.agent.javassist.convert.TransformBefore;
import org.hotswap.agent.javassist.convert.TransformCall;
import org.hotswap.agent.javassist.convert.TransformFieldAccess;
import org.hotswap.agent.javassist.convert.TransformNew;
import org.hotswap.agent.javassist.convert.TransformNewClass;
import org.hotswap.agent.javassist.convert.TransformReadField;
import org.hotswap.agent.javassist.convert.TransformWriteField;
import org.hotswap.agent.javassist.convert.Transformer;


public class CodeConverter {
    protected Transformer transformers = null;


    public void replaceNew(CtClass newClass,
                           CtClass calledClass, String calledMethod) {
        transformers = new TransformNew(transformers, newClass.getName(),
                                        calledClass.getName(), calledMethod);
    }


    public void replaceNew(CtClass oldClass, CtClass newClass) {
        transformers = new TransformNewClass(transformers, oldClass.getName(),
                                             newClass.getName());
    }


    public void redirectFieldAccess(CtField field,
                                    CtClass newClass, String newFieldname) {
        transformers = new TransformFieldAccess(transformers, field,
                                                newClass.getName(),
                                                newFieldname);
    }


    public void replaceFieldRead(CtField field,
                                 CtClass calledClass, String calledMethod) {
        transformers = new TransformReadField(transformers, field,
                                              calledClass.getName(),
                                              calledMethod);
    }


    public void replaceFieldWrite(CtField field,
                                  CtClass calledClass, String calledMethod) {
        transformers = new TransformWriteField(transformers, field,
                                               calledClass.getName(),
                                               calledMethod);
    }


    public void replaceArrayAccess(CtClass calledClass, ArrayAccessReplacementMethodNames names)
        throws NotFoundException
    {
       transformers = new TransformAccessArrayField(transformers, calledClass.getName(), names);
    }


    public void redirectMethodCall(CtMethod origMethod,
                                   CtMethod substMethod)
        throws CannotCompileException
    {
        String d1 = origMethod.getMethodInfo2().getDescriptor();
        String d2 = substMethod.getMethodInfo2().getDescriptor();
        if (!d1.equals(d2))
            throw new CannotCompileException("signature mismatch: "
                                             + substMethod.getLongName());

        int mod1 = origMethod.getModifiers();
        int mod2 = substMethod.getModifiers();
        if (Modifier.isStatic(mod1) != Modifier.isStatic(mod2)
            || (Modifier.isPrivate(mod1) && !Modifier.isPrivate(mod2))
            || origMethod.getDeclaringClass().isInterface()
               != substMethod.getDeclaringClass().isInterface())
            throw new CannotCompileException("invoke-type mismatch "
                                             + substMethod.getLongName());

        transformers = new TransformCall(transformers, origMethod,
                                         substMethod);
    }


    public void redirectMethodCall(String oldMethodName,
                                   CtMethod newMethod)
        throws CannotCompileException
    {
        transformers
            = new TransformCall(transformers, oldMethodName, newMethod);
    }


    public void insertBeforeMethod(CtMethod origMethod,
                                   CtMethod beforeMethod)
        throws CannotCompileException
    {
        try {
            transformers = new TransformBefore(transformers, origMethod,
                                               beforeMethod);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
    }


    public void insertAfterMethod(CtMethod origMethod,
                                  CtMethod afterMethod)
        throws CannotCompileException
    {
        try {
            transformers = new TransformAfter(transformers, origMethod,
                                               afterMethod);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
    }


    protected void doit(CtClass clazz, MethodInfo minfo, ConstPool cp)
        throws CannotCompileException
    {
       Transformer t;
        CodeAttribute codeAttr = minfo.getCodeAttribute();
        if (codeAttr == null || transformers == null)
            return;
        for (t = transformers; t != null; t = t.getNext())
            t.initialize(cp, clazz, minfo);

        CodeIterator iterator = codeAttr.iterator();
        while (iterator.hasNext()) {
            try {
                int pos = iterator.next();
                for (t = transformers; t != null; t = t.getNext())
                    pos = t.transform(clazz, pos, iterator, cp);
            }
            catch (BadBytecode e) {
                throw new CannotCompileException(e);
            }
        }

        int locals = 0;
        int stack = 0;
        for (t = transformers; t != null; t = t.getNext()) {
            int s = t.extraLocals();
            if (s > locals)
                locals = s;

            s = t.extraStack();
            if (s > stack)
                stack = s;
        }

        for (t = transformers; t != null; t = t.getNext())
            t.clean();

        if (locals > 0)
            codeAttr.setMaxLocals(codeAttr.getMaxLocals() + locals);

        if (stack > 0)
            codeAttr.setMaxStack(codeAttr.getMaxStack() + stack);

        try {
        	minfo.rebuildStackMapIf6(clazz.getClassPool(),
                                     clazz.getClassFile2());
        }
        catch (BadBytecode b) {
            throw new CannotCompileException(b.getMessage(), b);
        }
    }


    public interface ArrayAccessReplacementMethodNames
    {

       String byteOrBooleanRead();


       String byteOrBooleanWrite();


       String charRead();


       String charWrite();


       String doubleRead();


       String doubleWrite();


       String floatRead();


       String floatWrite();


       String intRead();


       String intWrite();


       String longRead();


       String longWrite();


       String objectRead();


       String objectWrite();


       String shortRead();


       String shortWrite();
    }


    public static class DefaultArrayAccessReplacementMethodNames
        implements ArrayAccessReplacementMethodNames
    {

        @Override
       public String byteOrBooleanRead()
       {
          return "arrayReadByteOrBoolean";
       }


        @Override
       public String byteOrBooleanWrite()
       {
          return "arrayWriteByteOrBoolean";
       }


        @Override
       public String charRead()
       {
          return "arrayReadChar";
       }


        @Override
       public String charWrite()
       {
          return "arrayWriteChar";
       }


        @Override
       public String doubleRead()
       {
          return "arrayReadDouble";
       }


        @Override
       public String doubleWrite()
       {
          return "arrayWriteDouble";
       }


        @Override
       public String floatRead()
       {
          return "arrayReadFloat";
       }


        @Override
       public String floatWrite()
       {
          return "arrayWriteFloat";
       }


        @Override
       public String intRead()
       {
          return "arrayReadInt";
       }


        @Override
       public String intWrite()
       {
          return "arrayWriteInt";
       }


        @Override
       public String longRead()
       {
          return "arrayReadLong";
       }


        @Override
       public String longWrite()
       {
          return "arrayWriteLong";
       }


        @Override
       public String objectRead()
       {
          return "arrayReadObject";
       }


        @Override
       public String objectWrite()
       {
          return "arrayWriteObject";
       }


        @Override
       public String shortRead()
       {
          return "arrayReadShort";
       }


        @Override
       public String shortWrite()
       {
          return "arrayWriteShort";
       }
    }
}
