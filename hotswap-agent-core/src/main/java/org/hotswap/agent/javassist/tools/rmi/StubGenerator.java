

package org.hotswap.agent.javassist.tools.rmi;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.CtField;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.CtMethod.ConstParameter;
import org.hotswap.agent.javassist.CtNewConstructor;
import org.hotswap.agent.javassist.CtNewMethod;
import org.hotswap.agent.javassist.Modifier;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.Translator;


public class StubGenerator implements Translator {
    private static final String fieldImporter = "importer";
    private static final String fieldObjectId = "objectId";
    private static final String accessorObjectId = "_getObjectId";
    private static final String sampleClass = "org.hotswap.agent.javassist.tools.rmi.Sample";

    private ClassPool classPool;
    private Map<String,CtClass> proxyClasses;
    private CtMethod forwardMethod;
    private CtMethod forwardStaticMethod;

    private CtClass[] proxyConstructorParamTypes;
    private CtClass[] interfacesForProxy;
    private CtClass[] exceptionForProxy;


    public StubGenerator() {
        proxyClasses = new Hashtable<String,CtClass>();
    }


    @Override
    public void start(ClassPool pool) throws NotFoundException {
        classPool = pool;
        CtClass c = pool.get(sampleClass);
        forwardMethod = c.getDeclaredMethod("forward");
        forwardStaticMethod = c.getDeclaredMethod("forwardStatic");

        proxyConstructorParamTypes
            = pool.get(new String[] { "org.hotswap.agent.javassist.tools.rmi.ObjectImporter",
                                         "int" });
        interfacesForProxy
            = pool.get(new String[] { "java.io.Serializable",
                                         "org.hotswap.agent.javassist.tools.rmi.Proxy" });
        exceptionForProxy
            = new CtClass[] { pool.get("org.hotswap.agent.javassist.tools.rmi.RemoteException") };
    }


    @Override
    public void onLoad(ClassPool pool, String classname) {}


    public boolean isProxyClass(String name) {
        return proxyClasses.get(name) != null;
    }


    public synchronized boolean makeProxyClass(Class<?> clazz)
        throws CannotCompileException, NotFoundException
    {
        String classname = clazz.getName();
        if (proxyClasses.get(classname) != null)
            return false;
        CtClass ctclazz = produceProxyClass(classPool.get(classname),
                                            clazz);
        proxyClasses.put(classname, ctclazz);
        modifySuperclass(ctclazz);
        return true;
    }

    private CtClass produceProxyClass(CtClass orgclass, Class<?> orgRtClass)
        throws CannotCompileException, NotFoundException
    {
        int modify = orgclass.getModifiers();
        if (Modifier.isAbstract(modify) || Modifier.isNative(modify)
            || !Modifier.isPublic(modify))
            throw new CannotCompileException(orgclass.getName()
                        + " must be public, non-native, and non-abstract.");

        CtClass proxy = classPool.makeClass(orgclass.getName(),
                                              orgclass.getSuperclass());

        proxy.setInterfaces(interfacesForProxy);

        CtField f
            = new CtField(classPool.get("org.hotswap.agent.javassist.tools.rmi.ObjectImporter"),
                          fieldImporter, proxy);
        f.setModifiers(Modifier.PRIVATE);
        proxy.addField(f, CtField.Initializer.byParameter(0));

        f = new CtField(CtClass.intType, fieldObjectId, proxy);
        f.setModifiers(Modifier.PRIVATE);
        proxy.addField(f, CtField.Initializer.byParameter(1));

        proxy.addMethod(CtNewMethod.getter(accessorObjectId, f));

        proxy.addConstructor(CtNewConstructor.defaultConstructor(proxy));
        CtConstructor cons
            = CtNewConstructor.skeleton(proxyConstructorParamTypes,
                                        null, proxy);
        proxy.addConstructor(cons);

        try {
            addMethods(proxy, orgRtClass.getMethods());
            return proxy;
        }
        catch (SecurityException e) {
            throw new CannotCompileException(e);
        }
    }

    private CtClass toCtClass(Class<?> rtclass) throws NotFoundException {
        String name;
        if (!rtclass.isArray())
            name = rtclass.getName();
        else {
            StringBuffer sbuf = new StringBuffer();
            do {
                sbuf.append("[]");
                rtclass = rtclass.getComponentType();
            } while(rtclass.isArray());
            sbuf.insert(0, rtclass.getName());
            name = sbuf.toString();
        }

        return classPool.get(name);
    }

    private CtClass[] toCtClass(Class<?>[] rtclasses) throws NotFoundException {
        int n = rtclasses.length;
        CtClass[] ctclasses = new CtClass[n];
        for (int i = 0; i < n; ++i)
            ctclasses[i] = toCtClass(rtclasses[i]);

        return ctclasses;
    }


    private void addMethods(CtClass proxy, Method[] ms)
        throws CannotCompileException, NotFoundException
    {
        CtMethod wmethod;
        for (int i = 0; i < ms.length; ++i) {
            Method m = ms[i];
            int mod = m.getModifiers();
            if (m.getDeclaringClass() != Object.class
                        && !Modifier.isFinal(mod))
                if (Modifier.isPublic(mod)) {
                    CtMethod body;
                    if (Modifier.isStatic(mod))
                        body = forwardStaticMethod;
                    else
                        body = forwardMethod;

                    wmethod
                        = CtNewMethod.wrapped(toCtClass(m.getReturnType()),
                                              m.getName(),
                                              toCtClass(m.getParameterTypes()),
                                              exceptionForProxy,
                                              body,
                                              ConstParameter.integer(i),
                                              proxy);
                    wmethod.setModifiers(mod);
                    proxy.addMethod(wmethod);
                }
                else if (!Modifier.isProtected(mod)
                         && !Modifier.isPrivate(mod))

                    throw new CannotCompileException(
                        "the methods must be public, protected, or private.");
        }
    }


    private void modifySuperclass(CtClass orgclass)
        throws CannotCompileException, NotFoundException
    {
        CtClass superclazz;
        for (;; orgclass = superclazz) {
            superclazz = orgclass.getSuperclass();
            if (superclazz == null)
                break;

            try {
                superclazz.getDeclaredConstructor(null);
                break;
            }
            catch (NotFoundException e) {
            }

            superclazz.addConstructor(
                        CtNewConstructor.defaultConstructor(superclazz));
        }
    }
}
