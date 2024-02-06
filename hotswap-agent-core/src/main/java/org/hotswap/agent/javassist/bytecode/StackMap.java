

package org.hotswap.agent.javassist.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

import org.hotswap.agent.javassist.CannotCompileException;


public class StackMap extends AttributeInfo {

    public static final String tag = "StackMap";



    StackMap(ConstPool cp, byte[] newInfo) {
        super(cp, tag, newInfo);
    }

    StackMap(ConstPool cp, int name_id, DataInputStream in)
        throws IOException
    {
        super(cp, name_id, in);
    }


    public int numOfEntries() {
    	return ByteArray.readU16bit(info, 0);
    }


    public static final int TOP = 0;


    public static final int INTEGER = 1;


    public static final int FLOAT = 2;


    public static final int DOUBLE = 3;


    public static final int LONG = 4;


    public static final int NULL = 5;


    public static final int THIS = 6;


    public static final int OBJECT = 7;


    public static final int UNINIT = 8;


    @Override
    public AttributeInfo copy(ConstPool newCp, Map<String,String> classnames) {
        Copier copier = new Copier(this, newCp, classnames);
        copier.visit();
        return copier.getStackMap();
    }


    public static class Walker {
        byte[] info;


        public Walker(StackMap sm) {
            info = sm.get();
        }


        public void visit() {
            int num = ByteArray.readU16bit(info, 0);
            int pos = 2;
            for (int i = 0; i < num; i++) {
                int offset = ByteArray.readU16bit(info, pos);
                int numLoc = ByteArray.readU16bit(info, pos + 2);
                pos = locals(pos + 4, offset, numLoc);
                int numStack = ByteArray.readU16bit(info, pos);
                pos = stack(pos + 2, offset, numStack);
            }
        }


        public int locals(int pos, int offset, int num) {
            return typeInfoArray(pos, offset, num, true);
        }


        public int stack(int pos, int offset, int num) {
            return typeInfoArray(pos, offset, num, false);
        }


        public int typeInfoArray(int pos, int offset, int num, boolean isLocals) {
            for (int k = 0; k < num; k++)
                pos = typeInfoArray2(k, pos);

            return pos;
        }

        int typeInfoArray2(int k, int pos) {
            byte tag = info[pos];
            if (tag == OBJECT) {
                int clazz = ByteArray.readU16bit(info, pos + 1);
                objectVariable(pos, clazz);
                pos += 3;
            }
            else if (tag == UNINIT) {
                int offsetOfNew = ByteArray.readU16bit(info, pos + 1);
                uninitialized(pos, offsetOfNew);
                pos += 3;
            }
            else {
                typeInfo(pos, tag);
                pos++;
            }

            return pos;
        }


        public void typeInfo(int pos, byte tag) {}


        public void objectVariable(int pos, int clazz) {}


        public void uninitialized(int pos, int offset) {}
    }

    static class Copier extends Walker {
        byte[] dest;
        ConstPool srcCp, destCp;
        Map<String,String> classnames;

        Copier(StackMap map, ConstPool newCp, Map<String,String> classnames) {
            super(map);
            srcCp = map.getConstPool();
            dest = new byte[info.length];
            destCp = newCp;
            this.classnames = classnames;
        }
        @Override
        public void visit() {
            int num = ByteArray.readU16bit(info, 0);
            ByteArray.write16bit(num, dest, 0);
            super.visit();
        }

        @Override
        public int locals(int pos, int offset, int num) {
            ByteArray.write16bit(offset, dest, pos - 4);
            return super.locals(pos, offset, num);
        }

        @Override
        public int typeInfoArray(int pos, int offset, int num, boolean isLocals) {
            ByteArray.write16bit(num, dest, pos - 2);
            return super.typeInfoArray(pos, offset, num, isLocals);
        }

        @Override
        public void typeInfo(int pos, byte tag) {
            dest[pos] = tag;
        }

        @Override
        public void objectVariable(int pos, int clazz) {
            dest[pos] = OBJECT;
            int newClazz = srcCp.copy(clazz, destCp, classnames);
            ByteArray.write16bit(newClazz, dest, pos + 1);
        }

        @Override
        public void uninitialized(int pos, int offset) {
            dest[pos] = UNINIT;
            ByteArray.write16bit(offset, dest, pos + 1);
        }

        public StackMap getStackMap() {
            return new StackMap(destCp, dest);
        }
    }


    public void insertLocal(int index, int tag, int classInfo)
        throws BadBytecode
    {
        byte[] data = new InsertLocal(this, index, tag, classInfo).doit();
        this.set(data);
    }

    static class SimpleCopy extends Walker {
        Writer writer;

        SimpleCopy(StackMap map) {
            super(map);
            writer = new Writer();
        }

        byte[] doit() {
            visit();
            return writer.toByteArray();
        }

        @Override
        public void visit() {
            int num = ByteArray.readU16bit(info, 0);
            writer.write16bit(num);
            super.visit();
        }

        @Override
        public int locals(int pos, int offset, int num) {
            writer.write16bit(offset);
            return super.locals(pos, offset, num);
        }

        @Override
        public int typeInfoArray(int pos, int offset, int num, boolean isLocals) {
            writer.write16bit(num);
            return super.typeInfoArray(pos, offset, num, isLocals);
        }

        @Override
        public void typeInfo(int pos, byte tag) {
            writer.writeVerifyTypeInfo(tag, 0);
        }

        @Override
        public void objectVariable(int pos, int clazz) {
            writer.writeVerifyTypeInfo(OBJECT, clazz);
        }

        @Override
        public void uninitialized(int pos, int offset) {
            writer.writeVerifyTypeInfo(UNINIT, offset);
        }
    }

    static class InsertLocal extends SimpleCopy {
        private int varIndex;
        private int varTag, varData;

        InsertLocal(StackMap map, int varIndex, int varTag, int varData) {
            super(map);
            this.varIndex = varIndex;
            this.varTag = varTag;
            this.varData = varData;
        }

        @Override
        public int typeInfoArray(int pos, int offset, int num, boolean isLocals) {
            if (!isLocals || num < varIndex)
                return super.typeInfoArray(pos, offset, num, isLocals);

            writer.write16bit(num + 1);
            for (int k = 0; k < num; k++) {
                if (k == varIndex)
                    writeVarTypeInfo();

                pos = typeInfoArray2(k, pos);
            }

            if (num == varIndex)
                writeVarTypeInfo();

            return pos;
        }

        private void writeVarTypeInfo() {
            if (varTag == OBJECT)
                writer.writeVerifyTypeInfo(OBJECT, varData);
            else if (varTag == UNINIT)
                writer.writeVerifyTypeInfo(UNINIT, varData);
            else
                writer.writeVerifyTypeInfo(varTag, 0);
        }
    }

    void shiftPc(int where, int gapSize, boolean exclusive)
        throws BadBytecode
    {
        new Shifter(this, where, gapSize, exclusive).visit();
    }

    static class Shifter extends Walker {
        private int where, gap;
        private boolean exclusive;

        public Shifter(StackMap smt, int where, int gap, boolean exclusive) {
            super(smt);
            this.where = where;
            this.gap = gap;
            this.exclusive = exclusive;
        }

        @Override
        public int locals(int pos, int offset, int num) {
            if (exclusive ? where <= offset : where < offset)
                ByteArray.write16bit(offset + gap, info, pos - 4);

            return super.locals(pos, offset, num);
        }

        @Override
        public void uninitialized(int pos, int offset) {
            if (where <= offset)
                ByteArray.write16bit(offset + gap, info, pos + 1);
        }
    }


    void shiftForSwitch(int where, int gapSize) throws BadBytecode {
        new SwitchShifter(this, where, gapSize).visit();
    }

    static class SwitchShifter extends Walker {
        private int where, gap;

        public SwitchShifter(StackMap smt, int where, int gap) {
            super(smt);
            this.where = where;
            this.gap = gap;
        }

        @Override
        public int locals(int pos, int offset, int num) {
            if (where == pos + offset)
                ByteArray.write16bit(offset - gap, info, pos - 4);
            else if (where == pos)
                ByteArray.write16bit(offset + gap, info, pos - 4);

            return super.locals(pos, offset, num);
        }
    }


     public void removeNew(int where) throws CannotCompileException {
         byte[] data = new NewRemover(this, where).doit();
         this.set(data);
    }

    static class NewRemover extends SimpleCopy {
        int posOfNew;

        NewRemover(StackMap map, int where) {
            super(map);
            posOfNew = where;
        }

        @Override
        public int stack(int pos, int offset, int num) {
            return stackTypeInfoArray(pos, offset, num);
        }

        private int stackTypeInfoArray(int pos, int offset, int num) {
            int p = pos;
            int count = 0;
            for (int k = 0; k < num; k++) {
                byte tag = info[p];
                if (tag == OBJECT)
                    p += 3;
                else if (tag == UNINIT) {
                    int offsetOfNew = ByteArray.readU16bit(info, p + 1);
                    if (offsetOfNew == posOfNew)
                        count++;

                    p += 3;
                }
                else
                    p++;
            }

            writer.write16bit(num - count);
            for (int k = 0; k < num; k++) {
                byte tag = info[pos];
                if (tag == OBJECT) {
                    int clazz = ByteArray.readU16bit(info, pos + 1);
                    objectVariable(pos, clazz);
                    pos += 3;
                }
                else if (tag == UNINIT) {
                    int offsetOfNew = ByteArray.readU16bit(info, pos + 1);
                    if (offsetOfNew != posOfNew)
                        uninitialized(pos, offsetOfNew);

                    pos += 3;
                }
                else {
                    typeInfo(pos, tag);
                    pos++;
                }
            }

            return pos;
        }
    }


    public void print(java.io.PrintWriter out) {
        new Printer(this, out).print();
    }

    static class Printer extends Walker {
        private java.io.PrintWriter writer;

        public Printer(StackMap map, java.io.PrintWriter out) {
            super(map);
            writer = out;
        }

        public void print() {
            int num = ByteArray.readU16bit(info, 0);
            writer.println(num + " entries");
            visit();
        }

        @Override
        public int locals(int pos, int offset, int num) {
            writer.println("  * offset " + offset);
            return super.locals(pos, offset, num);
        }
    }


    public static class Writer {


        private ByteArrayOutputStream output;


        public Writer() {
            output = new ByteArrayOutputStream();
        }


        public byte[] toByteArray() {
            return output.toByteArray();
        }


        public StackMap toStackMap(ConstPool cp) {
            return new StackMap(cp, output.toByteArray());
        }


        public void writeVerifyTypeInfo(int tag, int data) {
            output.write(tag);
            if (tag == StackMap.OBJECT || tag == StackMap.UNINIT)
                write16bit(data);
        }


        public void write16bit(int value) {
            output.write((value >>> 8) & 0xff);
            output.write(value & 0xff);
        }
    }
}
