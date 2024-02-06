

package org.hotswap.agent.javassist.bytecode;


public class AccessFlag {
    public static final int PUBLIC    = 0x0001;
    public static final int PRIVATE   = 0x0002;
    public static final int PROTECTED = 0x0004;
    public static final int STATIC    = 0x0008;
    public static final int FINAL     = 0x0010;
    public static final int SYNCHRONIZED = 0x0020;
    public static final int VOLATILE  = 0x0040;
    public static final int BRIDGE    = 0x0040;
    public static final int TRANSIENT = 0x0080;
    public static final int VARARGS   = 0x0080;
    public static final int NATIVE    = 0x0100;
    public static final int INTERFACE = 0x0200;
    public static final int ABSTRACT  = 0x0400;
    public static final int STRICT    = 0x0800;
    public static final int SYNTHETIC = 0x1000;
    public static final int ANNOTATION = 0x2000;
    public static final int ENUM      = 0x4000;
    public static final int MANDATED  = 0x8000;

    public static final int SUPER     = 0x0020;
    public static final int MODULE    = 0x8000;





    public static int setPublic(int accflags) {
        return (accflags & ~(PRIVATE | PROTECTED)) | PUBLIC;
    }


    public static int setProtected(int accflags) {
        return (accflags & ~(PRIVATE | PUBLIC)) | PROTECTED;
    }


    public static int setPrivate(int accflags) {
        return (accflags & ~(PROTECTED | PUBLIC)) | PRIVATE;
    }


    public static int setPackage(int accflags) {
        return (accflags & ~(PROTECTED | PUBLIC | PRIVATE));
    }


    public static boolean isPublic(int accflags) {
        return (accflags & PUBLIC) != 0;
    }


    public static boolean isProtected(int accflags) {
        return (accflags & PROTECTED) != 0;
    }


    public static boolean isPrivate(int accflags) {
        return (accflags & PRIVATE) != 0;
    }


    public static boolean isPackage(int accflags) {
        return (accflags & (PROTECTED | PUBLIC | PRIVATE)) == 0;
    }


    public static int clear(int accflags, int clearBit) {
        return accflags & ~clearBit;
    }


    public static int of(int modifier) {
        return modifier;
    }


    public static int toModifier(int accflags) {
        return accflags;
    }
}
