

package org.hotswap.agent.javassist.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CodeAttribute extends AttributeInfo implements Opcode {

    public static final String tag = "Code";



    private int maxStack;
    private int maxLocals;
    private ExceptionTable exceptions;
    private List<AttributeInfo> attributes;


    public CodeAttribute(ConstPool cp, int stack, int locals, byte[] code,
                         ExceptionTable etable)
    {
        super(cp, tag);
        maxStack = stack;
        maxLocals = locals;
        info = code;
        exceptions = etable;
        attributes = new ArrayList<AttributeInfo>();
    }


    private CodeAttribute(ConstPool cp, CodeAttribute src, Map<String,String> classnames)
        throws BadBytecode
    {
        super(cp, tag);

        maxStack = src.getMaxStack();
        maxLocals = src.getMaxLocals();
        exceptions = src.getExceptionTable().copy(cp, classnames);
        attributes = new ArrayList<AttributeInfo>();
        List<AttributeInfo> src_attr = src.getAttributes();
        int num = src_attr.size();
        for (int i = 0; i < num; ++i) {
            AttributeInfo ai = src_attr.get(i);
            attributes.add(ai.copy(cp, classnames));
        }

        info = src.copyCode(cp, classnames, exceptions, this);
    }

    CodeAttribute(ConstPool cp, int name_id, DataInputStream in)
        throws IOException
    {
        super(cp, name_id, (byte[])null);
        @SuppressWarnings("unused")
        int attr_len = in.readInt();

        maxStack = in.readUnsignedShort();
        maxLocals = in.readUnsignedShort();

        int code_len = in.readInt();
        info = new byte[code_len];
        in.readFully(info);

        exceptions = new ExceptionTable(cp, in);

        attributes = new ArrayList<AttributeInfo>();
        int num = in.readUnsignedShort();
        for (int i = 0; i < num; ++i)
            attributes.add(AttributeInfo.read(cp, in));
    }


    @Override
    public AttributeInfo copy(ConstPool newCp, Map<String,String> classnames)
        throws RuntimeCopyException
    {
        try {
            return new CodeAttribute(newCp, this, classnames);
        }
        catch (BadBytecode e) {
            throw new RuntimeCopyException("bad bytecode. fatal?");
        }
    }


    public static class RuntimeCopyException extends RuntimeException {

        private static final long serialVersionUID = 1L;


        public RuntimeCopyException(String s) {
            super(s);
        }
    }


    @Override
    public int length() {
        return 18 + info.length + exceptions.size() * 8
               + AttributeInfo.getLength(attributes);
    }

    @Override
    void write(DataOutputStream out) throws IOException {
        out.writeShort(name);
        out.writeInt(length() - 6);
        out.writeShort(maxStack);
        out.writeShort(maxLocals);
        out.writeInt(info.length);
        out.write(info);
        exceptions.write(out);
        out.writeShort(attributes.size());
        AttributeInfo.writeAll(attributes, out);
    }


    @Override
    public byte[] get() {
        throw new UnsupportedOperationException("CodeAttribute.get()");
    }


    @Override
    public void set(byte[] newinfo) {
        throw new UnsupportedOperationException("CodeAttribute.set()");
    }

    @Override
    void renameClass(String oldname, String newname) {
        AttributeInfo.renameClass(attributes, oldname, newname);
    }

    @Override
    void renameClass(Map<String,String> classnames) {
        AttributeInfo.renameClass(attributes, classnames);
    }

    @Override
    void getRefClasses(Map<String,String> classnames) {
        AttributeInfo.getRefClasses(attributes, classnames);
    }


    public String getDeclaringClass() {
        ConstPool cp = getConstPool();
        return cp.getClassName();
    }


    public int getMaxStack() {
        return maxStack;
    }


    public void setMaxStack(int value) {
        maxStack = value;
    }


    public int computeMaxStack() throws BadBytecode {
        maxStack = new CodeAnalyzer(this).computeMaxStack();
        return maxStack;
    }


    public int getMaxLocals() {
        return maxLocals;
    }


    public void setMaxLocals(int value) {
        maxLocals = value;
    }


    public int getCodeLength() {
        return info.length;
    }


    public byte[] getCode() {
        return info;
    }


    void setCode(byte[] newinfo) { super.set(newinfo); }


    public CodeIterator iterator() {
        return new CodeIterator(this);
    }


    public ExceptionTable getExceptionTable() { return exceptions; }


    public List<AttributeInfo> getAttributes() { return attributes; }


    public AttributeInfo getAttribute(String name) {
        return AttributeInfo.lookup(attributes, name);
    }


    public void setAttribute(StackMapTable smt) {
        AttributeInfo.remove(attributes, StackMapTable.tag);
        if (smt != null)
            attributes.add(smt);
    }


    public void setAttribute(StackMap sm) {
        AttributeInfo.remove(attributes, StackMap.tag);
        if (sm != null)
            attributes.add(sm);
    }


    private byte[] copyCode(ConstPool destCp, Map<String,String> classnames,
                            ExceptionTable etable, CodeAttribute destCa)
        throws BadBytecode
    {
        int len = getCodeLength();
        byte[] newCode = new byte[len];
        destCa.info = newCode;
        LdcEntry ldc = copyCode(this.info, 0, len, this.getConstPool(),
                                newCode, destCp, classnames);
        return LdcEntry.doit(newCode, ldc, etable, destCa);
    }

    private static LdcEntry copyCode(byte[] code, int beginPos, int endPos,
                                     ConstPool srcCp, byte[] newcode,
                                     ConstPool destCp, Map<String,String> classnameMap)
        throws BadBytecode
    {
        int i2, index;
        LdcEntry ldcEntry = null;

        for (int i = beginPos; i < endPos; i = i2) {
            i2 = CodeIterator.nextOpcode(code, i);
            byte c = code[i];
            newcode[i] = c;
            switch (c & 0xff) {
            case LDC_W :
            case LDC2_W :
            case GETSTATIC :
            case PUTSTATIC :
            case GETFIELD :
            case PUTFIELD :
            case INVOKEVIRTUAL :
            case INVOKESPECIAL :
            case INVOKESTATIC :
            case NEW :
            case ANEWARRAY :
            case CHECKCAST :
            case INSTANCEOF :
                copyConstPoolInfo(i + 1, code, srcCp, newcode, destCp,
                                  classnameMap);
                break;
            case LDC :
                index = code[i + 1] & 0xff;
                index = srcCp.copy(index, destCp, classnameMap);
                if (index < 0x100)
                    newcode[i + 1] = (byte)index;
                else {
                    newcode[i] = NOP;
                    newcode[i + 1] = NOP;
                    LdcEntry ldc = new LdcEntry();
                    ldc.where = i;
                    ldc.index = index;
                    ldc.next = ldcEntry;
                    ldcEntry = ldc;
                }
                break;
            case INVOKEINTERFACE :
                copyConstPoolInfo(i + 1, code, srcCp, newcode, destCp,
                                  classnameMap);
                newcode[i + 3] = code[i + 3];
                newcode[i + 4] = code[i + 4];
                break;
            case INVOKEDYNAMIC :
                copyConstPoolInfo(i + 1, code, srcCp, newcode, destCp,
                        classnameMap);
                newcode[i + 3] = 0;
                newcode[i + 4] = 0;
                break;
            case MULTIANEWARRAY :
                copyConstPoolInfo(i + 1, code, srcCp, newcode, destCp,
                                  classnameMap);
                newcode[i + 3] = code[i + 3];
                break;
            default :
                while (++i < i2)
                    newcode[i] = code[i];

                break;
            }
        }

        return ldcEntry;
    }

    private static void copyConstPoolInfo(int i, byte[] code, ConstPool srcCp,
                                          byte[] newcode, ConstPool destCp,
                                          Map<String,String> classnameMap) {
        int index = ((code[i] & 0xff) << 8) | (code[i + 1] & 0xff);
        index = srcCp.copy(index, destCp, classnameMap);
        newcode[i] = (byte)(index >> 8);
        newcode[i + 1] = (byte)index;
    }

    static class LdcEntry {
        LdcEntry next;
        int where;
        int index;

        static byte[] doit(byte[] code, LdcEntry ldc, ExceptionTable etable,
                           CodeAttribute ca)
            throws BadBytecode
        {
            if (ldc != null)
                code = CodeIterator.changeLdcToLdcW(code, etable, ca, ldc);



            return code;
        }
    }


    public void insertLocalVar(int where, int size) throws BadBytecode {
        CodeIterator ci = iterator();
        while (ci.hasNext())
            shiftIndex(ci, where, size);

        setMaxLocals(getMaxLocals() + size);
    }


    private static void shiftIndex(CodeIterator ci, int lessThan, int delta) throws BadBytecode {
        int index = ci.next();
        int opcode = ci.byteAt(index);
        if (opcode < ILOAD)
            return;
        else if (opcode < IASTORE) {
            if (opcode < ILOAD_0) {

                shiftIndex8(ci, index, opcode, lessThan, delta);
            }
            else if (opcode < IALOAD) {

                shiftIndex0(ci, index, opcode, lessThan, delta, ILOAD_0, ILOAD);
            }
            else if (opcode < ISTORE)
                return;
            else if (opcode < ISTORE_0) {

                shiftIndex8(ci, index, opcode, lessThan, delta);
            }
            else {

                shiftIndex0(ci, index, opcode, lessThan, delta, ISTORE_0, ISTORE);
            }
        }
        else if (opcode == IINC) {
            int var = ci.byteAt(index + 1);
            if (var < lessThan)
                return;

            var += delta;
            if (var < 0x100)
                ci.writeByte(var, index + 1);
            else {
                int plus = (byte)ci.byteAt(index + 2);
                int pos = ci.insertExGap(3);
                ci.writeByte(WIDE, pos - 3);
                ci.writeByte(IINC, pos - 2);
                ci.write16bit(var, pos - 1);
                ci.write16bit(plus, pos + 1);
            }
        }
        else if (opcode == RET)
            shiftIndex8(ci, index, opcode, lessThan, delta);
        else if (opcode == WIDE) {
            int var = ci.u16bitAt(index + 2);
            if (var < lessThan)
                return;

            var += delta;
            ci.write16bit(var, index + 2);
        }
    }

    private static void shiftIndex8(CodeIterator ci, int index, int opcode,
                                    int lessThan, int delta)
         throws BadBytecode
    {
        int var = ci.byteAt(index + 1);
        if (var < lessThan)
            return;

        var += delta;
        if (var < 0x100)
            ci.writeByte(var, index + 1);
        else {
            int pos = ci.insertExGap(2);
            ci.writeByte(WIDE, pos - 2);
            ci.writeByte(opcode, pos - 1);
            ci.write16bit(var, pos);
        }
    }

    private static void shiftIndex0(CodeIterator ci, int index, int opcode,
                                    int lessThan, int delta,
                                    int opcode_i_0, int opcode_i)
        throws BadBytecode
    {
        int var = (opcode - opcode_i_0) % 4;
        if (var < lessThan)
            return;

        var += delta;
        if (var < 4)
            ci.writeByte(opcode + delta, index);
        else {
            opcode = (opcode - opcode_i_0) / 4 + opcode_i;
            if (var < 0x100) {
                int pos = ci.insertExGap(1);
                ci.writeByte(opcode, pos - 1);
                ci.writeByte(var, pos);
            }
            else {
                int pos = ci.insertExGap(3);
                ci.writeByte(WIDE, pos - 1);
                ci.writeByte(opcode, pos);
                ci.write16bit(var, pos + 1);
            }
        }
    }
}
