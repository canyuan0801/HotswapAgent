/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package org.hotswap.agent.javassist.bytecode;


public class AccessFlag {
    public static final int PUBLIC    = 0x0001;
    public static final int PRIVATE   = 0x0002;
    public static final int PROTECTED = 0x0004;
    public static final int STATIC    = 0x0008;
    public static final int FINAL     = 0x0010;
    public static final int SYNCHRONIZED = 0x0020;
    public static final int VOLATILE  = 0x0040;
    public static final int BRIDGE    = 0x0040;     // for method_info
    public static final int TRANSIENT = 0x0080;
    public static final int VARARGS   = 0x0080;     // for method_info
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

    // Note: 0x0020 is assigned to both ACC_SUPER and ACC_SYNCHRONIZED
    // although java.lang.reflect.Modifier does not recognize ACC_SUPER.

    /**
     * Turns the public bit on.  The protected and private bits are
     * cleared.
     */
    public static int setPublic(int acc_flag) {
        return (acc_flag & ~(PRIVATE | PROTECTED)) | PUBLIC;
    }


    public static int setProtected(int acc_flag) {
        return (acc_flag & ~(PRIVATE | PUBLIC)) | PROTECTED;
    }


    public static int setPrivate(int acc_flag) {
        return (acc_flag & ~(PROTECTED | PUBLIC)) | PRIVATE;
    }


    public static int setPackage(int acc_flag) {
        return (acc_flag & ~(PROTECTED | PUBLIC | PRIVATE));
    }


    public static boolean isPublic(int acc_flag) {
        return (acc_flag & PUBLIC) != 0;
    }


    public static boolean isProtected(int acc_flag) {
        return (acc_flag & PROTECTED) != 0;
    }


    public static boolean isPrivate(int acc_flag) {
        return (acc_flag & PRIVATE) != 0;
    }


    public static boolean isPackage(int acc_flag) {
        return (acc_flag & (PROTECTED | PUBLIC | PRIVATE)) == 0;
    }


    public static int clear(int acc_flag, int clearBit) {
        return acc_flag & ~clearBit;
    }


    public static int of(int modifier) {
        return modifier;
    }


    public static int toModifier(int acc_flag) {
        return acc_flag;
    }
}
