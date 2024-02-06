

package org.hotswap.agent.javassist.util.proxy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.List;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.bytecode.ClassFile;


public class DefineClassHelper {

    private static abstract class Helper {
        abstract Class<?> defineClass(String name, byte[] b, int off, int len, Class<?> neighbor,
                                      ClassLoader loader, ProtectionDomain protectionDomain)
            throws ClassFormatError, CannotCompileException;
    }

    private static class Java11 extends JavaOther {
        Class<?> defineClass(String name, byte[] bcode, int off, int len, Class<?> neighbor,
                             ClassLoader loader, ProtectionDomain protectionDomain)
            throws ClassFormatError, CannotCompileException
        {
            if (neighbor != null)
                return toClass(neighbor, bcode);
            else {


                return super.defineClass(name, bcode, off, len, neighbor, loader, protectionDomain);
            }
        }
    }

    private static class Java9 extends Helper {
        final class ReferencedUnsafe {
            private final SecurityActions.TheUnsafe sunMiscUnsafeTheUnsafe;
            private final MethodHandle defineClass;

            ReferencedUnsafe(SecurityActions.TheUnsafe usf, MethodHandle meth) {
                this.sunMiscUnsafeTheUnsafe = usf;
                this.defineClass = meth;
            }

            Class<?> defineClass(String name, byte[] b, int off, int len,
                                 ClassLoader loader, ProtectionDomain protectionDomain)
                throws ClassFormatError
            {
                try {
                    if (getCallerClass.invoke(stack) != Java9.class)
                        throw new IllegalAccessError("Access denied for caller.");
                } catch (Exception e) {
                    throw new RuntimeException("cannot initialize", e);
                }
                try {
                    return (Class<?>) defineClass.invokeWithArguments(
                                sunMiscUnsafeTheUnsafe.theUnsafe,
                                name, b, off, len, loader, protectionDomain);
                } catch (Throwable e) {
                    if (e instanceof RuntimeException) throw (RuntimeException) e;
                    if (e instanceof ClassFormatError) throw (ClassFormatError) e;
                    throw new ClassFormatError(e.getMessage());
                }
            }
        }

        private final Object stack;
        private final Method getCallerClass;
        private final ReferencedUnsafe sunMiscUnsafe = getReferencedUnsafe();

        Java9 () {
            Class<?> stackWalkerClass = null;
            try {
                stackWalkerClass = Class.forName("java.lang.StackWalker");
            } catch (ClassNotFoundException e) {

            }
            if (stackWalkerClass != null) {
                try {
                    Class<?> optionClass = Class.forName("java.lang.StackWalker$Option");
                    stack = stackWalkerClass.getMethod("getInstance", optionClass)

                                            .invoke(null, optionClass.getEnumConstants()[0]);
                    getCallerClass = stackWalkerClass.getMethod("getCallerClass");
                } catch (Throwable e) {
                    throw new RuntimeException("cannot initialize", e);
                }
            } else {
                stack = null;
                getCallerClass = null;
            }
        }

        private final ReferencedUnsafe getReferencedUnsafe() {
            try {
                if (privileged != null && getCallerClass.invoke(stack) != this.getClass())
                    throw new IllegalAccessError("Access denied for caller.");
            } catch (Exception e) {
                throw new RuntimeException("cannot initialize", e);
            }
            try {
                SecurityActions.TheUnsafe usf = SecurityActions.getSunMiscUnsafeAnonymously();
                List<Method> defineClassMethod = usf.methods.get("defineClass");

                if (null == defineClassMethod)
                    return null;
                MethodHandle meth = MethodHandles.lookup().unreflect(defineClassMethod.get(0));
                return new ReferencedUnsafe(usf, meth);
            } catch (Throwable e) {
                throw new RuntimeException("cannot initialize", e);
            }
        }

        @Override
        Class<?> defineClass(String name, byte[] b, int off, int len, Class<?> neighbor,
                                    ClassLoader loader, ProtectionDomain protectionDomain)
            throws ClassFormatError
        {
            try {
                if (getCallerClass.invoke(stack) != DefineClassHelper.class)
                    throw new IllegalAccessError("Access denied for caller.");
            } catch (Exception e) {
                throw new RuntimeException("cannot initialize", e);
            }
            return sunMiscUnsafe.defineClass(name, b, off, len, loader,
                                             protectionDomain);
        }
    }

    private static class Java7 extends Helper {
        private final SecurityActions stack = SecurityActions.stack;
        private final MethodHandle defineClass = getDefineClassMethodHandle();
        private final MethodHandle getDefineClassMethodHandle() {
            if (privileged != null && stack.getCallerClass() != this.getClass())
                throw new IllegalAccessError("Access denied for caller.");
            try {
                return SecurityActions.getMethodHandle(ClassLoader.class, "defineClass",
                        new Class[] {
                            String.class, byte[].class, int.class, int.class,
                            ProtectionDomain.class
                        });
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("cannot initialize", e);
                }
        }

        @Override
        Class<?> defineClass(String name, byte[] b, int off, int len, Class<?> neighbor,
                ClassLoader loader, ProtectionDomain protectionDomain)
            throws ClassFormatError
        {
            if (stack.getCallerClass() != DefineClassHelper.class)
                throw new IllegalAccessError("Access denied for caller.");
            try {
                return (Class<?>) defineClass.invokeWithArguments(
                            loader, name, b, off, len, protectionDomain);
            } catch (Throwable e) {
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                if (e instanceof ClassFormatError) throw (ClassFormatError) e;
                throw new ClassFormatError(e.getMessage());
            }
        }
    }

    private static class JavaOther extends Helper {
        private final Method defineClass = getDefineClassMethod();
        private final SecurityActions stack = SecurityActions.stack;

        private final Method getDefineClassMethod() {
            if (privileged != null && stack.getCallerClass() != this.getClass())
                throw new IllegalAccessError("Access denied for caller.");
            try {
                return SecurityActions.getDeclaredMethod(ClassLoader.class, "defineClass",
                        new Class[] {
                                String.class, byte[].class, int.class, int.class, ProtectionDomain.class
                        });
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("cannot initialize", e);
            }
        }

        @Override
        Class<?> defineClass(String name, byte[] b, int off, int len, Class<?> neighbor,
                             ClassLoader loader, ProtectionDomain protectionDomain)
            throws ClassFormatError, CannotCompileException
        {
            Class<?> klass = stack.getCallerClass();
            if (klass != DefineClassHelper.class && klass != this.getClass())
                throw new IllegalAccessError("Access denied for caller.");
            try {
                SecurityActions.setAccessible(defineClass, true);
                return (Class<?>) defineClass.invoke(loader, new Object[] {
                            name, b, off, len, protectionDomain
                });
            } catch (Throwable e) {
                if (e instanceof ClassFormatError) throw (ClassFormatError) e;
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new CannotCompileException(e);
            }
            finally {
                SecurityActions.setAccessible(defineClass, false);
            }
        }
    }



    private static final Helper privileged = ClassFile.MAJOR_VERSION > ClassFile.JAVA_10
            ? new Java11()
            : ClassFile.MAJOR_VERSION >= ClassFile.JAVA_9
                ? new Java9()
                : ClassFile.MAJOR_VERSION >= ClassFile.JAVA_7 ? new Java7() : new JavaOther();


    public static Class<?> toClass(String className, Class<?> neighbor, ClassLoader loader,
                                   ProtectionDomain domain, byte[] bcode)
        throws CannotCompileException
    {
        try {
            return privileged.defineClass(className, bcode, 0, bcode.length,
                                          neighbor, loader, domain);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (CannotCompileException e) {
            throw e;
        }
        catch (ClassFormatError e) {
            Throwable t = e.getCause();
            throw new CannotCompileException(t == null ? e : t);
        }
        catch (Exception e) {
            throw new CannotCompileException(e);
        }
    }



    public static Class<?> toClass(Class<?> neighbor, byte[] bcode)
        throws CannotCompileException
    {
        try {
            DefineClassHelper.class.getModule().addReads(neighbor.getModule());
            Lookup lookup = MethodHandles.lookup();
            Lookup prvlookup = MethodHandles.privateLookupIn(neighbor, lookup);
            return prvlookup.defineClass(bcode);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new CannotCompileException(e.getMessage() + ": " + neighbor.getName()
                                             + " has no permission to define the class");
        }
    }


    public static Class<?> toClass(Lookup lookup, byte[] bcode)
        throws CannotCompileException
    {
        try {
            return lookup.defineClass(bcode);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new CannotCompileException(e.getMessage());
        }
    }


    static Class<?> toPublicClass(String className, byte[] bcode)
        throws CannotCompileException
    {
        try {
            Lookup lookup = MethodHandles.lookup();
            lookup = lookup.dropLookupMode(java.lang.invoke.MethodHandles.Lookup.PRIVATE);
            return lookup.defineClass(bcode);
        }
        catch (Throwable t) {
            throw new CannotCompileException(t);
        }
    }

    private DefineClassHelper() {}
}
