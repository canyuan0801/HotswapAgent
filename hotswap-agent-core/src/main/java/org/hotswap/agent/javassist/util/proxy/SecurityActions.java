
package org.hotswap.agent.javassist.util.proxy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hotswap.agent.javassist.bytecode.ClassFile;

class SecurityActions extends SecurityManager
{
    public static final SecurityActions stack = new SecurityActions();


    public Class<?> getCallerClass() {
        return getClassContext()[2];
    }

    static Method[] getDeclaredMethods(final Class<?> clazz)
    {
        if (System.getSecurityManager() == null)
            return clazz.getDeclaredMethods();
        else {
            return AccessController.doPrivileged(
                new PrivilegedAction<Method[]>() {
                    public Method[] run() {
                        return clazz.getDeclaredMethods();
                    }
                });
        }
    }

    static Constructor<?>[] getDeclaredConstructors(final Class<?> clazz)
    {
        if (System.getSecurityManager() == null)
            return clazz.getDeclaredConstructors();
        else {
            return AccessController.doPrivileged(
                new PrivilegedAction<Constructor<?>[]>() {
                    public Constructor<?>[] run() {
                        return clazz.getDeclaredConstructors();
                    }
                });
        }
    }

    static MethodHandle getMethodHandle(final Class<?> clazz, final
                String name, final Class<?>[] params) throws NoSuchMethodException
    {
        try {
            return AccessController.doPrivileged(
                new PrivilegedExceptionAction<MethodHandle>() {
                    public MethodHandle run() throws IllegalAccessException,
                            NoSuchMethodException, SecurityException {
                        Method rmet = clazz.getDeclaredMethod(name, params);
                        rmet.setAccessible(true);
                        MethodHandle meth = MethodHandles.lookup().unreflect(rmet);
                        rmet.setAccessible(false);
                        return meth;
                    }
                });
        }
        catch (PrivilegedActionException e) {
            if (e.getCause() instanceof NoSuchMethodException)
                throw (NoSuchMethodException) e.getCause();
            throw new RuntimeException(e.getCause());
        }
    }

    static Method getDeclaredMethod(final Class<?> clazz, final String name,
            final Class<?>[] types) throws NoSuchMethodException
    {
        if (System.getSecurityManager() == null)
            return clazz.getDeclaredMethod(name, types);
        else {
            try {
                return AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Method>() {
                        public Method run() throws Exception {
                            return clazz.getDeclaredMethod(name, types);
                        }
                    });
            }
            catch (PrivilegedActionException e) {
                if (e.getCause() instanceof NoSuchMethodException)
                    throw (NoSuchMethodException) e.getCause();

                throw new RuntimeException(e.getCause());
            }
        }
    }

    static Constructor<?> getDeclaredConstructor(final Class<?> clazz,
                                              final Class<?>[] types)
        throws NoSuchMethodException
    {
        if (System.getSecurityManager() == null)
            return clazz.getDeclaredConstructor(types);
        else {
            try {
                return AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Constructor<?>>() {
                        public Constructor<?> run() throws Exception {
                            return clazz.getDeclaredConstructor(types);
                        }
                    });
            }
            catch (PrivilegedActionException e) {
                if (e.getCause() instanceof NoSuchMethodException)
                    throw (NoSuchMethodException) e.getCause();

                throw new RuntimeException(e.getCause());
            }
        }
    }

    static void setAccessible(final AccessibleObject ao,
                              final boolean accessible)
    {
        if (System.getSecurityManager() == null)
            ao.setAccessible(accessible);
        else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    ao.setAccessible(accessible);
                    return null;
                }
            });
        }
    }

    static void set(final Field fld, final Object target, final Object value)
        throws IllegalAccessException
    {
        if (System.getSecurityManager() == null)
            fld.set(target, value);
        else {
            try {
                AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Void>() {
                        public Void run() throws Exception {
                            fld.set(target, value);
                            return null;
                        }
                    });
            }
            catch (PrivilegedActionException e) {
                if (e.getCause() instanceof NoSuchMethodException)
                    throw (IllegalAccessException) e.getCause();
                throw new RuntimeException(e.getCause());
            }
        }
    }

    static TheUnsafe getSunMiscUnsafeAnonymously() throws ClassNotFoundException
    {
        try {
            return AccessController.doPrivileged(
                new PrivilegedExceptionAction<TheUnsafe>() { public TheUnsafe run() throws
                        ClassNotFoundException, NoSuchFieldException, SecurityException,
                        IllegalArgumentException, IllegalAccessException {
                    Class<?> unsafe = Class.forName("sun.misc.Unsafe");
                    Field theUnsafe = unsafe.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    TheUnsafe usf = stack.new TheUnsafe(unsafe, theUnsafe.get(null));
                    theUnsafe.setAccessible(false);
                    disableWarning(usf);
                    return usf;
                }
            });
        }
        catch (PrivilegedActionException e) {
            if (e.getCause() instanceof ClassNotFoundException)
                throw (ClassNotFoundException) e.getCause();
            if (e.getCause() instanceof NoSuchFieldException)
                throw new ClassNotFoundException("No such instance.", e.getCause());
            if (e.getCause() instanceof IllegalAccessException
                    || e.getCause() instanceof IllegalAccessException
                    || e.getCause() instanceof SecurityException)
                throw new ClassNotFoundException("Security denied access.", e.getCause());
            throw new RuntimeException(e.getCause());
        }
    }

    class TheUnsafe
    {
        final Class<?> unsafe;
        final Object theUnsafe;
        final Map<String, List<Method>> methods =
                new HashMap<String, List<Method>>();

        TheUnsafe(Class<?> c, Object o)
        {
            this.unsafe = c;
            this.theUnsafe = o;
            for (Method m: unsafe.getDeclaredMethods()) {
                if (!methods.containsKey(m.getName())) {
                    methods.put(m.getName(), Collections.singletonList(m));
                    continue;
                }
                if (methods.get(m.getName()).size() == 1)
                    methods.put(m.getName(),
                            new ArrayList<Method>(methods.get(m.getName())));
                methods.get(m.getName()).add(m);
            }
        }

        private Method getM(String name, Object[] o)
        {
            return methods.get(name).get(0);
        }

        public Object call(String name, Object... args)
        {
            try {
                return getM(name, args).invoke(theUnsafe, args);
            } catch (Throwable t) {t.printStackTrace();}
            return null;
        }
    }

    static void disableWarning(TheUnsafe tu) {
        try {
            if (ClassFile.MAJOR_VERSION < ClassFile.JAVA_9)
                return;
            Class<?> cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            tu.call("putObjectVolatile", cls, tu.call("staticFieldOffset", logger), null);
        } catch (Exception e) {  }
    }
}

