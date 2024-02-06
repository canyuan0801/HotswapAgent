

package org.hotswap.agent.javassist.bytecode;


public interface Opcode {


    int AALOAD = 50;
    int AASTORE = 83;
    int ACONST_NULL = 1;
    int ALOAD = 25;
    int ALOAD_0 = 42;
    int ALOAD_1 = 43;
    int ALOAD_2 = 44;
    int ALOAD_3 = 45;
    int ANEWARRAY = 189;
    int ARETURN = 176;
    int ARRAYLENGTH = 190;
    int ASTORE = 58;
    int ASTORE_0 = 75;
    int ASTORE_1 = 76;
    int ASTORE_2 = 77;
    int ASTORE_3 = 78;
    int ATHROW = 191;
    int BALOAD = 51;
    int BASTORE = 84;
    int BIPUSH = 16;
    int CALOAD = 52;
    int CASTORE = 85;
    int CHECKCAST = 192;
    int D2F = 144;
    int D2I = 142;
    int D2L = 143;
    int DADD = 99;
    int DALOAD = 49;
    int DASTORE = 82;
    int DCMPG = 152;
    int DCMPL = 151;
    int DCONST_0 = 14;
    int DCONST_1 = 15;
    int DDIV = 111;
    int DLOAD = 24;
    int DLOAD_0 = 38;
    int DLOAD_1 = 39;
    int DLOAD_2 = 40;
    int DLOAD_3 = 41;
    int DMUL = 107;
    int DNEG = 119;
    int DREM = 115;
    int DRETURN = 175;
    int DSTORE = 57;
    int DSTORE_0 = 71;
    int DSTORE_1 = 72;
    int DSTORE_2 = 73;
    int DSTORE_3 = 74;
    int DSUB = 103;
    int DUP = 89;
    int DUP2 = 92;
    int DUP2_X1 = 93;
    int DUP2_X2 = 94;
    int DUP_X1 = 90;
    int DUP_X2 = 91;
    int F2D = 141;
    int F2I = 139;
    int F2L = 140;
    int FADD = 98;
    int FALOAD = 48;
    int FASTORE = 81;
    int FCMPG = 150;
    int FCMPL = 149;
    int FCONST_0 = 11;
    int FCONST_1 = 12;
    int FCONST_2 = 13;
    int FDIV = 110;
    int FLOAD = 23;
    int FLOAD_0 = 34;
    int FLOAD_1 = 35;
    int FLOAD_2 = 36;
    int FLOAD_3 = 37;
    int FMUL = 106;
    int FNEG = 118;
    int FREM = 114;
    int FRETURN = 174;
    int FSTORE = 56;
    int FSTORE_0 = 67;
    int FSTORE_1 = 68;
    int FSTORE_2 = 69;
    int FSTORE_3 = 70;
    int FSUB = 102;
    int GETFIELD = 180;
    int GETSTATIC = 178;
    int GOTO = 167;
    int GOTO_W = 200;
    int I2B = 145;
    int I2C = 146;
    int I2D = 135;
    int I2F = 134;
    int I2L = 133;
    int I2S = 147;
    int IADD = 96;
    int IALOAD = 46;
    int IAND = 126;
    int IASTORE = 79;
    int ICONST_0 = 3;
    int ICONST_1 = 4;
    int ICONST_2 = 5;
    int ICONST_3 = 6;
    int ICONST_4 = 7;
    int ICONST_5 = 8;
    int ICONST_M1 = 2;
    int IDIV = 108;
    int IFEQ = 153;
    int IFGE = 156;
    int IFGT = 157;
    int IFLE = 158;
    int IFLT = 155;
    int IFNE = 154;
    int IFNONNULL = 199;
    int IFNULL = 198;
    int IF_ACMPEQ = 165;
    int IF_ACMPNE = 166;
    int IF_ICMPEQ = 159;
    int IF_ICMPGE = 162;
    int IF_ICMPGT = 163;
    int IF_ICMPLE = 164;
    int IF_ICMPLT = 161;
    int IF_ICMPNE = 160;
    int IINC = 132;
    int ILOAD = 21;
    int ILOAD_0 = 26;
    int ILOAD_1 = 27;
    int ILOAD_2 = 28;
    int ILOAD_3 = 29;
    int IMUL = 104;
    int INEG = 116;
    int INSTANCEOF = 193;
    int INVOKEDYNAMIC = 186;
    int INVOKEINTERFACE = 185;
    int INVOKESPECIAL = 183;
    int INVOKESTATIC = 184;
    int INVOKEVIRTUAL = 182;
    int IOR = 128;
    int IREM = 112;
    int IRETURN = 172;
    int ISHL = 120;
    int ISHR = 122;
    int ISTORE = 54;
    int ISTORE_0 = 59;
    int ISTORE_1 = 60;
    int ISTORE_2 = 61;
    int ISTORE_3 = 62;
    int ISUB = 100;
    int IUSHR = 124;
    int IXOR = 130;
    int JSR = 168;
    int JSR_W = 201;
    int L2D = 138;
    int L2F = 137;
    int L2I = 136;
    int LADD = 97;
    int LALOAD = 47;
    int LAND = 127;
    int LASTORE = 80;
    int LCMP = 148;
    int LCONST_0 = 9;
    int LCONST_1 = 10;
    int LDC = 18;
    int LDC2_W = 20;
    int LDC_W = 19;
    int LDIV = 109;
    int LLOAD = 22;
    int LLOAD_0 = 30;
    int LLOAD_1 = 31;
    int LLOAD_2 = 32;
    int LLOAD_3 = 33;
    int LMUL = 105;
    int LNEG = 117;
    int LOOKUPSWITCH = 171;
    int LOR = 129;
    int LREM = 113;
    int LRETURN = 173;
    int LSHL = 121;
    int LSHR = 123;
    int LSTORE = 55;
    int LSTORE_0 = 63;
    int LSTORE_1 = 64;
    int LSTORE_2 = 65;
    int LSTORE_3 = 66;
    int LSUB = 101;
    int LUSHR = 125;
    int LXOR = 131;
    int MONITORENTER = 194;
    int MONITOREXIT = 195;
    int MULTIANEWARRAY = 197;
    int NEW = 187;
    int NEWARRAY = 188;
    int NOP = 0;
    int POP = 87;
    int POP2 = 88;
    int PUTFIELD = 181;
    int PUTSTATIC = 179;
    int RET = 169;
    int RETURN = 177;
    int SALOAD = 53;
    int SASTORE = 86;
    int SIPUSH = 17;
    int SWAP = 95;
    int TABLESWITCH = 170;
    int WIDE = 196;



    int T_BOOLEAN = 4;
    int T_CHAR = 5;
    int T_FLOAT = 6;
    int T_DOUBLE = 7;
    int T_BYTE = 8;
    int T_SHORT = 9;
    int T_INT = 10;
    int T_LONG = 11;


    int[] STACK_GROW = {
        0,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        2,
        2,
        1,
        1,
        1,
        2,
        2,
        1,
        1,
        1,
        1,
        2,
        1,
        2,
        1,
        2,
        1,
        1,
        1,
        1,
        1,
        2,
        2,
        2,
        2,
        1,
        1,
        1,
        1,
        2,
        2,
        2,
        2,
        1,
        1,
        1,
        1,
        -1,
        0,
        -1,
        0,
        -1,
        -1,
        -1,
        -1,
        -1,
        -2,
        -1,
        -2,
        -1,
        -1,
        -1,
        -1,
        -1,
        -2,
        -2,
        -2,
        -2,
        -1,
        -1,
        -1,
        -1,
        -2,
        -2,
        -2,
        -2,
        -1,
        -1,
        -1,
        -1,
        -3,
        -4,
        -3,
        -4,
        -3,
        -3,
        -3,
        -3,
        -1,
        -2,
        1,
        1,
        1,
        2,
        2,
        2,
        0,
        -1,
        -2,
        -1,
        -2,
        -1,
        -2,
        -1,
        -2,
        -1,
        -2,
        -1,
        -2,
        -1,
        -2,
        -1,
        -2,
        -1,
        -2,
        -1,
        -2,
        0,
        0,
        0,
        0,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -2,
        -1,
        -2,
        -1,
        -2,
        0,
        1,
        0,
        1,
        -1,
        -1,
        0,
        0,
        1,
        1,
        -1,
        0,
        -1,
        0,
        0,
        0,
        -3,
        -1,
        -1,
        -3,
        -3,
        -1,
        -1,
        -1,
        -1,
        -1,
        -1,
        -2,
        -2,
        -2,
        -2,
        -2,
        -2,
        -2,
        -2,
        0,
        1,
        0,
        -1,
        -1,
        -1,
        -2,
        -1,
        -2,
        -1,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        1,
        0,
        0,
        0,
        -1,
        0,
        0,
        -1,
        -1,
        0,
        0,
        -1,
        -1,
        0,
        1
    };
}
