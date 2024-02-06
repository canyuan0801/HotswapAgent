

package org.hotswap.agent.javassist.bytecode;

import java.util.ArrayList;
import java.util.List;


public class CodeIterator implements Opcode {
    protected CodeAttribute codeAttr;
    protected byte[] bytecode;
    protected int endPos;
    protected int currentPos;
    protected int mark;

    protected CodeIterator(CodeAttribute ca) {
        codeAttr = ca;
        bytecode = ca.getCode();
        begin();
    }


    public void begin() {
        currentPos = mark = 0;
        endPos = getCodeLength();
    }


    public void move(int index) {
        currentPos = index;
    }


    public void setMark(int index) {
        mark = index;
    }


    public int getMark() { return mark; }


    public CodeAttribute get() {
        return codeAttr;
    }


    public int getCodeLength() {
        return bytecode.length;
    }


    public int byteAt(int index) { return bytecode[index] & 0xff; }


    public int signedByteAt(int index) { return bytecode[index]; }


    public void writeByte(int value, int index) {
        bytecode[index] = (byte)value;
    }


    public int u16bitAt(int index) {
        return ByteArray.readU16bit(bytecode, index);
    }


    public int s16bitAt(int index) {
        return ByteArray.readS16bit(bytecode, index);
    }


    public void write16bit(int value, int index) {
        ByteArray.write16bit(value, bytecode, index);
    }


    public int s32bitAt(int index) {
        return ByteArray.read32bit(bytecode, index);
    }


    public void write32bit(int value, int index) {
        ByteArray.write32bit(value, bytecode, index);
    }


    public void write(byte[] code, int index) {
        int len = code.length;
        for (int j = 0; j < len; ++j)
            bytecode[index++] = code[j];
    }


    public boolean hasNext() { return currentPos < endPos; }


    public int next() throws BadBytecode {
        int pos = currentPos;
        currentPos = nextOpcode(bytecode, pos);
        return pos;
    }


    public int lookAhead() {
        return currentPos;
    }


    public int skipConstructor() throws BadBytecode {
        return skipSuperConstructor0(-1);
    }


    public int skipSuperConstructor() throws BadBytecode {
        return skipSuperConstructor0(0);
    }


    public int skipThisConstructor() throws BadBytecode {
        return skipSuperConstructor0(1);
    }


    private int skipSuperConstructor0(int skipThis) throws BadBytecode {
        begin();
        ConstPool cp = codeAttr.getConstPool();
        String thisClassName = codeAttr.getDeclaringClass();
        int nested = 0;
        while (hasNext()) {
            int index = next();
            int c = byteAt(index);
            if (c == NEW)
                ++nested;
            else if (c == INVOKESPECIAL) {
                int mref = ByteArray.readU16bit(bytecode, index + 1);
                if (cp.getMethodrefName(mref).equals(MethodInfo.nameInit))
                    if (--nested < 0) {
                        if (skipThis < 0)
                            return index;

                        String cname = cp.getMethodrefClassName(mref);
                        if (cname.equals(thisClassName) == (skipThis > 0))
                            return index;

                        break;
                    }
            }
        }

        begin();
        return -1;
    }


    public int insert(byte[] code)
        throws BadBytecode
    {
        return insert0(currentPos, code, false);
    }


    public void insert(int pos, byte[] code) throws BadBytecode {
        insert0(pos, code, false);
    }


    public int insertAt(int pos, byte[] code) throws BadBytecode {
        return insert0(pos, code, false);
    }


    public int insertEx(byte[] code)
        throws BadBytecode
    {
        return insert0(currentPos, code, true);
    }


    public void insertEx(int pos, byte[] code) throws BadBytecode {
        insert0(pos, code, true);
    }


    public int insertExAt(int pos, byte[] code) throws BadBytecode {
        return insert0(pos, code, true);
    }


    private int insert0(int pos, byte[] code, boolean exclusive)
        throws BadBytecode
    {
        int len = code.length;
        if (len <= 0)
            return pos;


        pos = insertGapAt(pos, len, exclusive).position;

        int p = pos;
        for (int j = 0; j < len; ++j)
            bytecode[p++] = code[j];

        return pos;
    }


    public int insertGap(int length) throws BadBytecode {
        return insertGapAt(currentPos, length, false).position;
    }


    public int insertGap(int pos, int length) throws BadBytecode {
        return insertGapAt(pos, length, false).length;
    }


    public int insertExGap(int length) throws BadBytecode {
        return insertGapAt(currentPos, length, true).position;
    }


    public int insertExGap(int pos, int length) throws BadBytecode {
        return insertGapAt(pos, length, true).length;
    }


    public static class Gap {

        public int position;


        public int length;
    }


    public Gap insertGapAt(int pos, int length, boolean exclusive)
        throws BadBytecode
    {

        Gap gap = new Gap();
        if (length <= 0) {
            gap.position = pos;
            gap.length = 0;
            return gap;
        }

        byte[] c;
        int length2;
        if (bytecode.length + length > Short.MAX_VALUE) {

            c = insertGapCore0w(bytecode, pos, length, exclusive,
                                get().getExceptionTable(), codeAttr, gap);
            pos = gap.position;
            length2 = length;
        }
        else {
            int cur = currentPos;
            c = insertGapCore0(bytecode, pos, length, exclusive,
                                      get().getExceptionTable(), codeAttr);

            length2 = c.length - bytecode.length;
            gap.position = pos;
            gap.length = length2;
            if (cur >= pos)
                currentPos = cur + length2;

            if (mark > pos || (mark == pos && exclusive))
                mark += length2;
        }

        codeAttr.setCode(c);
        bytecode = c;
        endPos = getCodeLength();
        updateCursors(pos, length2);
        return gap;
    }


    protected void updateCursors(int pos, int length) {

    }


    public void insert(ExceptionTable et, int offset) {
        codeAttr.getExceptionTable().add(0, et, offset);
    }


    public int append(byte[] code) {
        int size = getCodeLength();
        int len = code.length;
        if (len <= 0)
            return size;

        appendGap(len);
        byte[] dest = bytecode;
        for (int i = 0; i < len; ++i)
            dest[i + size] = code[i];

        return size;
    }


    public void appendGap(int gapLength) {
        byte[] code = bytecode;
        int codeLength = code.length;
        byte[] newcode = new byte[codeLength + gapLength];

        int i;
        for (i = 0; i < codeLength; ++i)
            newcode[i] = code[i];

        for (i = codeLength; i < codeLength + gapLength; ++i)
            newcode[i] = NOP;

        codeAttr.setCode(newcode);
        bytecode = newcode;
        endPos = getCodeLength();
    }


    public void append(ExceptionTable et, int offset) {
        ExceptionTable table = codeAttr.getExceptionTable();
        table.add(table.size(), et, offset);
    }


    private static final int opcodeLength[] = {
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 2, 3,
        3, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3,
        3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 0, 0, 1, 1, 1, 1, 1, 1, 3, 3,
        3, 3, 3, 3, 3, 5, 5, 3, 2, 3, 1, 1, 3, 3, 1, 1, 0, 4, 3, 3,
        5, 5
    };



    static int nextOpcode(byte[] code, int index)
        throws BadBytecode
    {
        int opcode;
        try {
            opcode = code[index] & 0xff;
        }
        catch (IndexOutOfBoundsException e) {
            throw new BadBytecode("invalid opcode address");
        }

        try {
            int len = opcodeLength[opcode];
            if (len > 0)
                return index + len;
            else if (opcode == WIDE)
                if (code[index + 1] == (byte)IINC)
                    return index + 6;
                else
                    return index + 4;
            int index2 = (index & ~3) + 8;
            if (opcode == LOOKUPSWITCH) {
                int npairs = ByteArray.read32bit(code, index2);
                return index2 + npairs * 8 + 4;
            }
            else if (opcode == TABLESWITCH) {
                int low = ByteArray.read32bit(code, index2);
                int high = ByteArray.read32bit(code, index2 + 4);
                return index2 + (high - low + 1) * 4 + 8;
            }
        }
        catch (IndexOutOfBoundsException e) {
        }


        throw new BadBytecode(opcode);
    }



    static class AlignmentException extends Exception {


        private static final long serialVersionUID = 1L;}


    static byte[] insertGapCore0(byte[] code, int where, int gapLength,
                                 boolean exclusive, ExceptionTable etable, CodeAttribute ca)
        throws BadBytecode
    {
        if (gapLength <= 0)
            return code;

        try {
            return insertGapCore1(code, where, gapLength, exclusive, etable, ca);
        }
        catch (AlignmentException e) {
            try {
                return insertGapCore1(code, where, (gapLength + 3) & ~3,
                                  exclusive, etable, ca);
            }
            catch (AlignmentException e2) {
                throw new RuntimeException("fatal error?");
            }
        }
    }

    private static byte[] insertGapCore1(byte[] code, int where, int gapLength,
                                         boolean exclusive, ExceptionTable etable,
                                         CodeAttribute ca)
        throws BadBytecode, AlignmentException
    {
        int codeLength = code.length;
        byte[] newcode = new byte[codeLength + gapLength];
        insertGap2(code, where, gapLength, codeLength, newcode, exclusive);
        etable.shiftPc(where, gapLength, exclusive);
        LineNumberAttribute na
            = (LineNumberAttribute)ca.getAttribute(LineNumberAttribute.tag);
        if (na != null)
            na.shiftPc(where, gapLength, exclusive);

        LocalVariableAttribute va = (LocalVariableAttribute)ca.getAttribute(
                                                LocalVariableAttribute.tag);
        if (va != null)
            va.shiftPc(where, gapLength, exclusive);

        LocalVariableAttribute vta
            = (LocalVariableAttribute)ca.getAttribute(
                                              LocalVariableAttribute.typeTag);
        if (vta != null)
            vta.shiftPc(where, gapLength, exclusive);

        StackMapTable smt = (StackMapTable)ca.getAttribute(StackMapTable.tag);
        if (smt != null)
            smt.shiftPc(where, gapLength, exclusive);

        StackMap sm = (StackMap)ca.getAttribute(StackMap.tag);
        if (sm != null)
            sm.shiftPc(where, gapLength, exclusive);

        return newcode;
    }

    private static void insertGap2(byte[] code, int where, int gapLength,
                        int endPos, byte[] newcode, boolean exclusive)
        throws BadBytecode, AlignmentException
    {
        int nextPos;
        int i = 0;
        int j = 0;
        for (; i < endPos; i = nextPos) {
            if (i == where) {
                int j2 = j + gapLength;
                while (j < j2)
                    newcode[j++] = NOP;
            }

            nextPos = nextOpcode(code, i);
            int inst = code[i] & 0xff;

            if ((153 <= inst && inst <= 168)
                || inst == IFNULL || inst == IFNONNULL) {

                int offset = (code[i + 1] << 8) | (code[i + 2] & 0xff);
                offset = newOffset(i, offset, where, gapLength, exclusive);
                newcode[j] = code[i];
                ByteArray.write16bit(offset, newcode, j + 1);
                j += 3;
            }
            else if (inst == GOTO_W || inst == JSR_W) {

                int offset = ByteArray.read32bit(code, i + 1);
                offset = newOffset(i, offset, where, gapLength, exclusive);
                newcode[j++] = code[i];
                ByteArray.write32bit(offset, newcode, j);
                j += 4;
            }
            else if (inst == TABLESWITCH) {
                if (i != j && (gapLength & 3) != 0)
                    throw new AlignmentException();

                int i2 = (i & ~3) + 4;






                j = copyGapBytes(newcode, j, code, i, i2);

                int defaultbyte = newOffset(i, ByteArray.read32bit(code, i2),
                                            where, gapLength, exclusive);
                ByteArray.write32bit(defaultbyte, newcode, j);
                int lowbyte = ByteArray.read32bit(code, i2 + 4);
                ByteArray.write32bit(lowbyte, newcode, j + 4);
                int highbyte = ByteArray.read32bit(code, i2 + 8);
                ByteArray.write32bit(highbyte, newcode, j + 8);
                j += 12;
                int i0 = i2 + 12;
                i2 = i0 + (highbyte - lowbyte + 1) * 4;
                while (i0 < i2) {
                    int offset = newOffset(i, ByteArray.read32bit(code, i0),
                                           where, gapLength, exclusive);
                    ByteArray.write32bit(offset, newcode, j);
                    j += 4;
                    i0 += 4;
                }
            }
            else if (inst == LOOKUPSWITCH) {
                if (i != j && (gapLength & 3) != 0)
                    throw new AlignmentException();

                int i2 = (i & ~3) + 4;







                j = copyGapBytes(newcode, j, code, i, i2);

                int defaultbyte = newOffset(i, ByteArray.read32bit(code, i2),
                                            where, gapLength, exclusive);
                ByteArray.write32bit(defaultbyte, newcode, j);
                int npairs = ByteArray.read32bit(code, i2 + 4);
                ByteArray.write32bit(npairs, newcode, j + 4);
                j += 8;
                int i0 = i2 + 8;
                i2 = i0 + npairs * 8;
                while (i0 < i2) {
                    ByteArray.copy32bit(code, i0, newcode, j);
                    int offset = newOffset(i,
                                        ByteArray.read32bit(code, i0 + 4),
                                        where, gapLength, exclusive);
                    ByteArray.write32bit(offset, newcode, j + 4);
                    j += 8;
                    i0 += 8;
                }
            }
            else
                while (i < nextPos)
                    newcode[j++] = code[i++];
            }
    }


    private static int copyGapBytes(byte[] newcode, int j, byte[] code, int i, int iEnd) {
        switch (iEnd - i) {
        case 4:
            newcode[j++] = code[i++];
        case 3:
            newcode[j++] = code[i++];
        case 2:
            newcode[j++] = code[i++];
        case 1:
            newcode[j++] = code[i++];
        default:
        }

        return j;
    }

    private static int newOffset(int i, int offset, int where,
                                 int gapLength, boolean exclusive) {
        int target = i + offset;
        if (i < where) {
            if (where < target || (exclusive && where == target))
                offset += gapLength;
        }
        else if (i == where) {


            if (target < where)
                offset -= gapLength;
        }
        else
            if (target < where || (!exclusive && where == target))
                offset -= gapLength;

        return offset;
    }

    static class Pointers {
        int cursor;
        int mark0, mark;
        ExceptionTable etable;
        LineNumberAttribute line;
        LocalVariableAttribute vars, types;
        StackMapTable stack;
        StackMap stack2;

        Pointers(int cur, int m, int m0, ExceptionTable et, CodeAttribute ca) {
            cursor = cur;
            mark = m;
            mark0 = m0;
            etable = et;
            line = (LineNumberAttribute)ca.getAttribute(LineNumberAttribute.tag);
            vars = (LocalVariableAttribute)ca.getAttribute(LocalVariableAttribute.tag);
            types = (LocalVariableAttribute)ca.getAttribute(LocalVariableAttribute.typeTag);
            stack = (StackMapTable)ca.getAttribute(StackMapTable.tag);
            stack2 = (StackMap)ca.getAttribute(StackMap.tag);
        }

        void shiftPc(int where, int gapLength, boolean exclusive) throws BadBytecode {
            if (where < cursor || (where == cursor && exclusive))
                cursor += gapLength;

            if (where < mark || (where == mark && exclusive))
                mark += gapLength;

            if (where < mark0 || (where == mark0 && exclusive))
                mark0 += gapLength;

            etable.shiftPc(where, gapLength, exclusive);
            if (line != null)
                line.shiftPc(where, gapLength, exclusive);

            if (vars != null)
                vars.shiftPc(where, gapLength, exclusive);

            if (types != null)
                types.shiftPc(where, gapLength, exclusive);

            if (stack != null)
                stack.shiftPc(where, gapLength, exclusive);

            if (stack2 != null)
                stack2.shiftPc(where, gapLength, exclusive);
        }

        void shiftForSwitch(int where, int gapLength) throws BadBytecode {
            if (stack != null)
                stack.shiftForSwitch(where, gapLength);

            if (stack2 != null)
                stack2.shiftForSwitch(where, gapLength);
        }
    }


    static byte[] changeLdcToLdcW(byte[] code, ExceptionTable etable,
                                  CodeAttribute ca, CodeAttribute.LdcEntry ldcs)
        throws BadBytecode
    {
        Pointers pointers = new Pointers(0, 0, 0, etable, ca);
        List<Branch> jumps = makeJumpList(code, code.length, pointers);
        while (ldcs != null) {
            addLdcW(ldcs, jumps);
            ldcs = ldcs.next;
        }

        byte[] r = insertGap2w(code, 0, 0, false, jumps, pointers);
        return r;
    }

    private static void addLdcW(CodeAttribute.LdcEntry ldcs, List<Branch> jumps) {
        int where = ldcs.where;
        LdcW ldcw = new LdcW(where, ldcs.index);
        int s = jumps.size();
        for (int i = 0; i < s; i++)
            if (where < jumps.get(i).orgPos) {
                jumps.add(i, ldcw);
                return;
            }

        jumps.add(ldcw);
    }


    private byte[] insertGapCore0w(byte[] code, int where, int gapLength, boolean exclusive,
                                   ExceptionTable etable, CodeAttribute ca, Gap newWhere)
        throws BadBytecode
    {
        if (gapLength <= 0)
            return code;

        Pointers pointers = new Pointers(currentPos, mark, where, etable, ca);
        List<Branch> jumps = makeJumpList(code, code.length, pointers);
        byte[] r = insertGap2w(code, where, gapLength, exclusive, jumps, pointers);
        currentPos = pointers.cursor;
        mark = pointers.mark;
        int where2 = pointers.mark0;
        if (where2 == currentPos && !exclusive)
            currentPos += gapLength;

        if (exclusive)
            where2 -= gapLength;

        newWhere.position = where2;
        newWhere.length = gapLength;
        return r;
    }

    private static byte[] insertGap2w(byte[] code, int where, int gapLength,
                                      boolean exclusive, List<Branch> jumps, Pointers ptrs)
        throws BadBytecode
    {
        if (gapLength > 0) {
            ptrs.shiftPc(where, gapLength, exclusive);
            for (Branch b:jumps)
                b.shift(where, gapLength, exclusive);
        }

        boolean unstable = true;
        do {
            while (unstable) {
                unstable = false;
                for (Branch b:jumps) {
                    if (b.expanded()) {
                        unstable = true;
                        int p = b.pos;
                        int delta = b.deltaSize();
                        ptrs.shiftPc(p, delta, false);
                        for (Branch bb:jumps)
                            bb.shift(p, delta, false);
                    }
                }
            }

            for (Branch b:jumps) {
                int diff = b.gapChanged();
                if (diff > 0) {
                    unstable = true;
                    int p = b.pos;
                    ptrs.shiftPc(p, diff, false);
                    for (Branch bb:jumps)
                        bb.shift(p, diff, false);
                }
            }
        } while (unstable);

        return makeExapndedCode(code, jumps, where, gapLength);
    }

    private static List<Branch> makeJumpList(byte[] code, int endPos, Pointers ptrs)
        throws BadBytecode
    {
        List<Branch> jumps = new ArrayList<Branch>();
        int nextPos;
        for (int i = 0; i < endPos; i = nextPos) {
            nextPos = nextOpcode(code, i);
            int inst = code[i] & 0xff;

            if ((153 <= inst && inst <= 168)
                    || inst == IFNULL || inst == IFNONNULL) {

                int offset = (code[i + 1] << 8) | (code[i + 2] & 0xff);
                Branch b;
                if (inst == GOTO || inst == JSR)
                    b = new Jump16(i, offset);
                else
                    b = new If16(i, offset);

                jumps.add(b);
            }
            else if (inst == GOTO_W || inst == JSR_W) {

                int offset = ByteArray.read32bit(code, i + 1);
                jumps.add(new Jump32(i, offset));
            }
            else if (inst == TABLESWITCH) {
                int i2 = (i & ~3) + 4;
                int defaultbyte = ByteArray.read32bit(code, i2);
                int lowbyte = ByteArray.read32bit(code, i2 + 4);
                int highbyte = ByteArray.read32bit(code, i2 + 8);
                int i0 = i2 + 12;
                int size = highbyte - lowbyte + 1;
                int[] offsets = new int[size];
                for (int j = 0; j < size; j++) {
                    offsets[j] = ByteArray.read32bit(code, i0);
                    i0 += 4;
                }

                jumps.add(new Table(i, defaultbyte, lowbyte, highbyte, offsets, ptrs));
            }
            else if (inst == LOOKUPSWITCH) {
                int i2 = (i & ~3) + 4;
                int defaultbyte = ByteArray.read32bit(code, i2);
                int npairs = ByteArray.read32bit(code, i2 + 4);
                int i0 = i2 + 8;
                int[] matches = new int[npairs];
                int[] offsets = new int[npairs];
                for (int j = 0; j < npairs; j++) {
                    matches[j] = ByteArray.read32bit(code, i0);
                    offsets[j] = ByteArray.read32bit(code, i0 + 4);
                    i0 += 8;
                }

                jumps.add(new Lookup(i, defaultbyte, matches, offsets, ptrs));
            }
        }

        return jumps;
    }

    private static byte[] makeExapndedCode(byte[] code, List<Branch> jumps,
                                           int where, int gapLength)
        throws BadBytecode
    {
        int n = jumps.size();
        int size = code.length + gapLength;
        for (Branch b:jumps)
            size += b.deltaSize();

        byte[] newcode = new byte[size];
        int src = 0, dest = 0, bindex = 0;
        int len = code.length;
        Branch b;
        int bpos;
        if (0 < n) {
            b = jumps.get(0);
            bpos = b.orgPos;
        }
        else {
            b = null;
            bpos = len;
        }

        while (src < len) {
            if (src == where) {
                int pos2 = dest + gapLength;
                while (dest < pos2)
                    newcode[dest++] = NOP;
            }

            if (src != bpos)
                newcode[dest++] = code[src++];
            else {
                int s = b.write(src, code, dest, newcode);
                src += s;
                dest += s + b.deltaSize();
                if (++bindex < n) {
                    b = jumps.get(bindex);
                    bpos = b.orgPos;
                }
                else  {
                    b = null;
                    bpos = len;
                }
            }
        }

        return newcode;
    }

    static abstract class Branch {
        int pos, orgPos;
        Branch(int p) { pos = orgPos = p; }
        void shift(int where, int gapLength, boolean exclusive) {
            if (where < pos || (where == pos && exclusive))
                pos += gapLength;
        }

        static int shiftOffset(int i, int offset, int where,
                               int gapLength, boolean exclusive) {
            int target = i + offset;
            if (i < where) {
                if (where < target || (exclusive && where == target))
                    offset += gapLength;
            }
            else if (i == where) {


                if (target < where && exclusive)
                    offset -= gapLength;
                else if (where < target && !exclusive)
                    offset += gapLength;
            }
            else
                if (target < where || (!exclusive && where == target))
                    offset -= gapLength;

            return offset;
        }

        boolean expanded() { return false; }
        int gapChanged() { return 0; }
        int deltaSize() { return 0; }


        abstract int write(int srcPos, byte[] code, int destPos, byte[] newcode) throws BadBytecode;
    }


    static class LdcW extends Branch {
        int index;
        boolean state;
        LdcW(int p, int i) {
            super(p);
            index = i;
            state = true;
        }

        @Override
        boolean expanded() {
            if (state) {
                state = false;
                return true;
            }
            return false;
        }

        @Override
        int deltaSize() { return 1; }

        @Override
        int write(int srcPos, byte[] code, int destPos, byte[] newcode) {
            newcode[destPos] = LDC_W;
            ByteArray.write16bit(index, newcode, destPos + 1);
            return 2;
        }
    }

    static abstract class Branch16 extends Branch {
        int offset;
        int state;
        static final int BIT16 = 0;
        static final int EXPAND = 1;
        static final int BIT32 = 2;

        Branch16(int p, int off) {
            super(p);
            offset = off;
            state = BIT16;
        }

        @Override
        void shift(int where, int gapLength, boolean exclusive) {
            offset = shiftOffset(pos, offset, where, gapLength, exclusive);
            super.shift(where, gapLength, exclusive);
            if (state == BIT16)
                if (offset < Short.MIN_VALUE || Short.MAX_VALUE < offset)
                    state = EXPAND;
        }

        @Override
        boolean expanded() {
            if (state == EXPAND) {
                state = BIT32;
                return true;
            }
            return false;
        }

        @Override
        abstract int deltaSize();
        abstract void write32(int src, byte[] code, int dest, byte[] newcode);

        @Override
        int write(int src, byte[] code, int dest, byte[] newcode) {
            if (state == BIT32)
                write32(src, code, dest, newcode);
            else {
                newcode[dest] = code[src];
                ByteArray.write16bit(offset, newcode, dest + 1);
            }

            return 3;
        }
    }


    static class Jump16 extends Branch16 {
        Jump16(int p, int off) {
            super(p, off);
        }

        @Override
        int deltaSize() {
            return state == BIT32 ? 2 : 0;
        }

        @Override
        void write32(int src, byte[] code, int dest, byte[] newcode) {
            newcode[dest] = (byte)(((code[src] & 0xff) == GOTO) ? GOTO_W : JSR_W);
            ByteArray.write32bit(offset, newcode, dest + 1);
        }
    }


    static class If16 extends Branch16 {
        If16(int p, int off) {
            super(p, off);
        }

        @Override
        int deltaSize() {
            return state == BIT32 ? 5 : 0;
        }

        @Override
        void write32(int src, byte[] code, int dest, byte[] newcode) {
            newcode[dest] = (byte)opcode(code[src] & 0xff);
            newcode[dest + 1] = 0;
            newcode[dest + 2] = 8;
            newcode[dest + 3] = (byte)GOTO_W;
            ByteArray.write32bit(offset - 3, newcode, dest + 4);
        }

        int opcode(int op) {
            if (op == IFNULL)
                return IFNONNULL;
            else if (op == IFNONNULL)
                return IFNULL;
            if (((op - IFEQ) & 1) == 0)
                return op + 1;
            return op - 1;
        }
    }

    static class Jump32 extends Branch {
        int offset;

        Jump32(int p, int off) {
            super(p);
            offset = off;
        }

        @Override
        void shift(int where, int gapLength, boolean exclusive) {
            offset = shiftOffset(pos, offset, where, gapLength, exclusive);
            super.shift(where, gapLength, exclusive);
        }

        @Override
        int write(int src, byte[] code, int dest, byte[] newcode) {
            newcode[dest] = code[src];
            ByteArray.write32bit(offset, newcode, dest + 1);
            return 5;
        }
    }

    static abstract class Switcher extends Branch {
        int gap, defaultByte;
        int[] offsets;
        Pointers pointers;

        Switcher(int pos, int defaultByte, int[] offsets, Pointers ptrs) {
            super(pos);
            this.gap = 3 - (pos & 3);
            this.defaultByte = defaultByte;
            this.offsets = offsets;
            this.pointers = ptrs;
        }

        @Override
        void shift(int where, int gapLength, boolean exclusive) {
            int p = pos;
            defaultByte = shiftOffset(p, defaultByte, where, gapLength, exclusive);
            int num = offsets.length;
            for (int i = 0; i < num; i++)
                offsets[i] = shiftOffset(p, offsets[i], where, gapLength, exclusive);

            super.shift(where, gapLength, exclusive);
        }

        @Override
        int gapChanged() {
            int newGap = 3 - (pos & 3);
            if (newGap > gap) {
                int diff = newGap - gap;
                gap = newGap;
                return diff;
            }

            return 0;
        }

        @Override
        int deltaSize() {
            return gap - (3 - (orgPos & 3));
        }

        @Override
        int write(int src, byte[] code, int dest, byte[] newcode) throws BadBytecode {
            int padding = 3 - (pos & 3);
            int nops = gap - padding;
            int bytecodeSize = 5 + (3 - (orgPos & 3)) + tableSize();
            if (nops > 0)
                adjustOffsets(bytecodeSize, nops);

            newcode[dest++] = code[src];
            while (padding-- > 0)
                newcode[dest++] = 0;

            ByteArray.write32bit(defaultByte, newcode, dest);
            int size = write2(dest + 4, newcode);
            dest += size + 4;
            while (nops-- > 0)
                newcode[dest++] = NOP;

            return 5 + (3 - (orgPos & 3)) + size;
        }

        abstract int write2(int dest, byte[] newcode);
        abstract int tableSize();


        void adjustOffsets(int size, int nops) throws BadBytecode {
            pointers.shiftForSwitch(pos + size, nops);
            if (defaultByte == size)
                defaultByte -= nops;

            for (int i = 0; i < offsets.length; i++)
                if (offsets[i] == size)
                    offsets[i] -= nops;
        }
    }

    static class Table extends Switcher {
        int low, high;

        Table(int pos, int defaultByte, int low, int high, int[] offsets, Pointers ptrs) {
            super(pos, defaultByte, offsets, ptrs);
            this.low = low;
            this.high = high;
        }

        @Override
        int write2(int dest, byte[] newcode) {
            ByteArray.write32bit(low, newcode, dest);
            ByteArray.write32bit(high, newcode, dest + 4);
            int n = offsets.length;
            dest += 8;
            for (int i = 0; i < n; i++) {
                ByteArray.write32bit(offsets[i], newcode, dest);
                dest += 4;
            }

            return 8 + 4 * n;
        }

        @Override
        int tableSize() { return 8 + 4 * offsets.length; }
    }

    static class Lookup extends Switcher {
        int[] matches;

        Lookup(int pos, int defaultByte, int[] matches, int[] offsets, Pointers ptrs) {
            super(pos, defaultByte, offsets, ptrs);
            this.matches = matches;
        }

        @Override
        int write2(int dest, byte[] newcode) {
            int n = matches.length;
            ByteArray.write32bit(n, newcode, dest);
            dest += 4;
            for (int i = 0; i < n; i++) {
                ByteArray.write32bit(matches[i], newcode, dest);
                ByteArray.write32bit(offsets[i], newcode, dest + 4);
                dest += 8;
            }

            return 4 + 8 * n;
        }

        @Override
        int tableSize() { return 4 + 8 * matches.length; }
    }
}
