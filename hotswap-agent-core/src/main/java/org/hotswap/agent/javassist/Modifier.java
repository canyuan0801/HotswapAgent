

package org.hotswap.agent.javassist;

import org.hotswap.agent.javassist.bytecode.AccessFlag;


public class Modifier {
    public static final int PUBLIC    = AccessFlag.PUBLIC;
    public static final int PRIVATE   = AccessFlag.PRIVATE;
    public static final int PROTECTED = AccessFlag.PROTECTED;
    public static final int STATIC    = AccessFlag.STATIC;
    public static final int FINAL     = AccessFlag.FINAL;
    public static final int SYNCHRONIZED = AccessFlag.SYNCHRONIZED;
    public static final int VOLATILE  = AccessFlag.VOLATILE;
    public static final int VARARGS = AccessFlag.VARARGS;
    public static final int TRANSIENT = AccessFlag.TRANSIENT;
    public static final int NATIVE    = AccessFlag.NATIVE;
    public static final int INTERFACE = AccessFlag.INTERFACE;
    public static final int ABSTRACT  = AccessFlag.ABSTRACT;
    public static final int STRICT    = AccessFlag.STRICT;
    public static final int ANNOTATION = AccessFlag.ANNOTATION;
    public static final int ENUM      = AccessFlag.ENUM;


    public static boolean isPublic(int mod) {
        return (mod & PUBLIC) != 0;
    }


    public static boolean isPrivate(int mod) {
        return (mod & PRIVATE) != 0;
    }


    public static boolean isProtected(int mod) {
        return (mod & PROTECTED) != 0;
    }


    public static boolean isPackage(int mod) {
        return (mod & (PUBLIC | PRIVATE | PROTECTED)) == 0;
    }


    public static boolean isStatic(int mod) {
        return (mod & STATIC) != 0;
    }


    public static boolean isFinal(int mod) {
        return (mod & FINAL) != 0;
    }


    public static boolean isSynchronized(int mod) {
        return (mod & SYNCHRONIZED) != 0;
    }


    public static boolean isVolatile(int mod) {
        return (mod & VOLATILE) != 0;
    }


    public static boolean isTransient(int mod) {
        return (mod & TRANSIENT) != 0;
    }


    public static boolean isNative(int mod) {
        return (mod & NATIVE) != 0;
    }


    public static boolean isInterface(int mod) {
        return (mod & INTERFACE) != 0;
    }


    public static boolean isAnnotation(int mod) {
        return (mod & ANNOTATION) != 0;
    }


    public static boolean isEnum(int mod) {
        return (mod & ENUM) != 0;
    }


    public static boolean isAbstract(int mod) {
        return (mod & ABSTRACT) != 0;
    }


    public static boolean isStrict(int mod) {
        return (mod & STRICT) != 0;
    }


    public static boolean isVarArgs(int mod)  {
        return (mod & VARARGS) != 0;
    }


    public static int setPublic(int mod) {
        return (mod & ~(PRIVATE | PROTECTED)) | PUBLIC;
    }


    public static int setProtected(int mod) {
        return (mod & ~(PRIVATE | PUBLIC)) | PROTECTED;
    }


    public static int setPrivate(int mod) {
        return (mod & ~(PROTECTED | PUBLIC)) | PRIVATE;
    }


    public static int setPackage(int mod) {
        return (mod & ~(PROTECTED | PUBLIC | PRIVATE));
    }


    public static int clear(int mod, int clearBit) {
        return mod & ~clearBit;
    }


    public static String toString(int mod) {
        return java.lang.reflect.Modifier.toString(mod);
    }
}
