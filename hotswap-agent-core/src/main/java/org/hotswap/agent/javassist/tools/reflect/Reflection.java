

package org.hotswap.agent.javassist.tools.reflect;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CodeConverter;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtField;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.CtMethod.ConstParameter;
import org.hotswap.agent.javassist.CtNewMethod;
import org.hotswap.agent.javassist.Modifier;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.Translator;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.ClassFile;
import org.hotswap.agent.javassist.bytecode.MethodInfo;


public class Reflection implements Translator {

    static final String classobjectField = "_classobject";
    static final String classobjectAccessor = "_getClass";
    static final String metaobjectField = "_metaobject";
    static final String metaobjectGetter = "_getMetaobject";
    static final String metaobjectSetter = "_setMetaobject";
    static final String readPrefix = "_r_";
    static final String writePrefix = "_w_";

    static final String metaobjectClassName = "org.hotswap.agent.javassist.tools.reflect.Metaobject";
    static final String classMetaobjectClassName
        = "org.hotswap.agenta.javassist.tools.reflect.ClassMetaobject";

    protected CtMethod trapMethod, trapStaticMethod;
    protected CtMethod trapRead, trapWrite;
    protected CtClass[] readParam;

    protected ClassPool classPool;
    protected CodeConverter converter;

    private boolean isExcluded(String name) {
        return name.startsWith(ClassMetaobject.methodPrefix)
            || name.equals(classobjectAccessor)
            || name.equals(metaobjectSetter)
            || name.equals(metaobjectGetter)
            || name.startsWith(readPrefix)
            || name.startsWith(writePrefix);
    }

    
    public Reflection() {
        classPool = null;
        converter = new CodeConverter();
    }

    
    @Override
    public void start(ClassPool pool) throws NotFoundException {
        classPool = pool;
        final String msg
            = "org.hotswap.agent.javassist.tools.reflect.Sample is not found or broken.";
        try {
            CtClass c = classPool.get("org.hotswap.agenta.javassist.tools.reflect.Sample");
            rebuildClassFile(c.getClassFile());
            trapMethod = c.getDeclaredMethod("trap");
            trapStaticMethod = c.getDeclaredMethod("trapStatic");
            trapRead = c.getDeclaredMethod("trapRead");
            trapWrite = c.getDeclaredMethod("trapWrite");
            readParam
                = new CtClass[] { classPool.get("java.lang.Object") };
        }
        catch (NotFoundException e) {
            throw new RuntimeException(msg);
        } catch (BadBytecode e) {
            throw new RuntimeException(msg);
        }
    }

    
    @Override
    public void onLoad(ClassPool pool, String classname)
        throws CannotCompileException, NotFoundException
    {
        CtClass clazz = pool.get(classname);
        clazz.instrument(converter);
    }

    
    public boolean makeReflective(String classname,
                                  String metaobject, String metaclass)
        throws CannotCompileException, NotFoundException
    {
        return makeReflective(classPool.get(classname),
                              classPool.get(metaobject),
                              classPool.get(metaclass));
    }

    
    public boolean makeReflective(Class<?> clazz,
                                  Class<?> metaobject, Class<?> metaclass)
        throws CannotCompileException, NotFoundException
    {
        return makeReflective(clazz.getName(), metaobject.getName(),
                              metaclass.getName());
    }

    
    public boolean makeReflective(CtClass clazz,
                                  CtClass metaobject, CtClass metaclass)
        throws CannotCompileException, CannotReflectException,
               NotFoundException
    {
        if (clazz.isInterface())
            throw new CannotReflectException(
                    "Cannot reflect an interface: " + clazz.getName());

        if (clazz.subclassOf(classPool.get(classMetaobjectClassName)))
            throw new CannotReflectException(
                "Cannot reflect a subclass of ClassMetaobject: "
                + clazz.getName());

        if (clazz.subclassOf(classPool.get(metaobjectClassName)))
            throw new CannotReflectException(
                "Cannot reflect a subclass of Metaobject: "
                + clazz.getName());

        registerReflectiveClass(clazz);
        return modifyClassfile(clazz, metaobject, metaclass);
    }

    
    private void registerReflectiveClass(CtClass clazz) {
        CtField[] fs = clazz.getDeclaredFields();
        for (int i = 0; i < fs.length; ++i) {
            CtField f = fs[i];
            int mod = f.getModifiers();
            if ((mod & Modifier.PUBLIC) != 0 && (mod & Modifier.FINAL) == 0) {
                String name = f.getName();
                converter.replaceFieldRead(f, clazz, readPrefix + name);
                converter.replaceFieldWrite(f, clazz, writePrefix + name);
            }
        }
    }

    private boolean modifyClassfile(CtClass clazz, CtClass metaobject,
                                    CtClass metaclass)
        throws CannotCompileException, NotFoundException
    {
        if (clazz.getAttribute("Reflective") != null)
            return false;       
        clazz.setAttribute("Reflective", new byte[0]);

        CtClass mlevel = classPool.get("org.hotswap.agent.javassist.tools.reflect.Metalevel");
        boolean addMeta = !clazz.subtypeOf(mlevel);
        if (addMeta)
            clazz.addInterface(mlevel);

        processMethods(clazz, addMeta);
        processFields(clazz);

        CtField f;
        if (addMeta) {
            f = new CtField(classPool.get("org.hotswap.agent.javassist.tools.reflect.Metaobject"),
                            metaobjectField, clazz);
            f.setModifiers(Modifier.PROTECTED);
            clazz.addField(f, CtField.Initializer.byNewWithParams(metaobject));

            clazz.addMethod(CtNewMethod.getter(metaobjectGetter, f));
            clazz.addMethod(CtNewMethod.setter(metaobjectSetter, f));
        }

        f = new CtField(classPool.get("org.hotswap.agent.javassist.tools.reflect.ClassMetaobject"),
                        classobjectField, clazz);
        f.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
        clazz.addField(f, CtField.Initializer.byNew(metaclass,
                                        new String[] { clazz.getName() }));

        clazz.addMethod(CtNewMethod.getter(classobjectAccessor, f));
        return true;
    }

    private void processMethods(CtClass clazz, boolean dontSearch)
        throws CannotCompileException, NotFoundException
    {
        CtMethod[] ms = clazz.getMethods();
        for (int i = 0; i < ms.length; ++i) {
            CtMethod m = ms[i];
            int mod = m.getModifiers();
            if (Modifier.isPublic(mod) && !Modifier.isAbstract(mod))
                processMethods0(mod, clazz, m, i, dontSearch);
        }
    }

    private void processMethods0(int mod, CtClass clazz,
                        CtMethod m, int identifier, boolean dontSearch)
        throws CannotCompileException, NotFoundException
    {
        CtMethod body;
        String name = m.getName();

        if (isExcluded(name))   
            return;             

        CtMethod m2;
        if (m.getDeclaringClass() == clazz) {
            if (Modifier.isNative(mod))
                return;

            m2 = m;
            if (Modifier.isFinal(mod)) {
                mod &= ~Modifier.FINAL;
                m2.setModifiers(mod);
            }
        }
        else {
            if (Modifier.isFinal(mod))
                return;

            mod &= ~Modifier.NATIVE;
            m2 = CtNewMethod.delegator(findOriginal(m, dontSearch), clazz);
            m2.setModifiers(mod);
            clazz.addMethod(m2);
        }

        m2.setName(ClassMetaobject.methodPrefix + identifier
                      + "_" + name);

        if (Modifier.isStatic(mod))
            body = trapStaticMethod;
        else
            body = trapMethod;

        CtMethod wmethod
            = CtNewMethod.wrapped(m.getReturnType(), name,
                                  m.getParameterTypes(), m.getExceptionTypes(),
                                  body, ConstParameter.integer(identifier),
                                  clazz);
        wmethod.setModifiers(mod);
        clazz.addMethod(wmethod);
    }

    private CtMethod findOriginal(CtMethod m, boolean dontSearch)
        throws NotFoundException
    {
        if (dontSearch)
            return m;

        String name = m.getName();
        CtMethod[] ms = m.getDeclaringClass().getDeclaredMethods();
        for (int i = 0; i < ms.length; ++i) {
            String orgName = ms[i].getName();
            if (orgName.endsWith(name)
                && orgName.startsWith(ClassMetaobject.methodPrefix)
                && ms[i].getSignature().equals(m.getSignature()))
                return ms[i];
        }

        return m;
    }

    private void processFields(CtClass clazz)
        throws CannotCompileException, NotFoundException
    {
        CtField[] fs = clazz.getDeclaredFields();
        for (int i = 0; i < fs.length; ++i) {
            CtField f = fs[i];
            int mod = f.getModifiers();
            if ((mod & Modifier.PUBLIC) != 0 && (mod & Modifier.FINAL) == 0) {
                mod |= Modifier.STATIC;
                String name = f.getName();
                CtClass ftype = f.getType();
                CtMethod wmethod
                    = CtNewMethod.wrapped(ftype, readPrefix + name,
                                          readParam, null, trapRead,
                                          ConstParameter.string(name),
                                          clazz);
                wmethod.setModifiers(mod);
                clazz.addMethod(wmethod);
                CtClass[] writeParam = new CtClass[2];
                writeParam[0] = classPool.get("java.lang.Object");
                writeParam[1] = ftype;
                wmethod = CtNewMethod.wrapped(CtClass.voidType,
                                writePrefix + name,
                                writeParam, null, trapWrite,
                                ConstParameter.string(name), clazz);
                wmethod.setModifiers(mod);
                clazz.addMethod(wmethod);
            }
        }
    }

    public void rebuildClassFile(ClassFile cf) throws BadBytecode {
        if (ClassFile.MAJOR_VERSION < ClassFile.JAVA_6)
            return;

        for (MethodInfo mi:cf.getMethods())
            mi.rebuildStackMap(classPool);
    }
}
