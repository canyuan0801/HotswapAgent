

package org.hotswap.agent.javassist.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.hotswap.agent.javassist.CannotCompileException;


public class StackMapTable extends AttributeInfo {

    public static final String tag = "StackMapTable";


    StackMapTable(ConstPool cp, byte[] newInfo) {
        super(cp, tag, newInfo);
    }

    StackMapTable(ConstPool cp, int name_id, DataInputStream in)
        throws IOException
    {
        super(cp, name_id, in);
    }


    @Override
    public AttributeInfo copy(ConstPool newCp, Map<String,String> classnames)
        throws RuntimeCopyException
    {
        try {
            return new StackMapTable(newCp,
                            new Copier(this.constPool, info, newCp, classnames).doit());
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
    void write(DataOutputStream out) throws IOException {
        super.write(out);
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


    public static class Walker {
        byte[] info;
        int numOfEntries;


        public Walker(StackMapTable smt) {
            this(smt.get());
        }


        public Walker(byte[] data) {
            info = data;
            numOfEntries = ByteArray.readU16bit(data, 0);
        }


        public final int size() { return numOfEntries; }


        public void parse() throws BadBytecode {
            int n = numOfEntries;
            int pos = 2;
            for (int i = 0; i < n; i++)
                pos = stackMapFrames(pos, i);
        }


        int stackMapFrames(int pos, int nth) throws BadBytecode {
            int type = info[pos] & 0xff;
            if (type < 64) {
                sameFrame(pos, type);
                pos++;
            }
            else if (type < 128)
                pos = sameLocals(pos, type);
            else if (type < 247)
                throw new BadBytecode("bad frame_type in StackMapTable");
            else if (type == 247)
                pos = sameLocals(pos, type);
            else if (type < 251) {
                int offset = ByteArray.readU16bit(info, pos + 1);
                chopFrame(pos, offset, 251 - type);
                pos += 3;
            }
            else if (type == 251) {
                int offset = ByteArray.readU16bit(info, pos + 1);
                sameFrame(pos, offset);
                pos += 3;
            }
            else if (type < 255)
                pos = appendFrame(pos, type);
            else
                pos = fullFrame(pos);

            return pos;
        }


        public void sameFrame(int pos, int offsetDelta) throws BadBytecode {}

        private int sameLocals(int pos, int type) throws BadBytecode {
            int top = pos;
            int offset;
            if (type < 128)
                offset = type - 64;
            else {
                offset = ByteArray.readU16bit(info, pos + 1);
                pos += 2;
            }

            int tag = info[pos + 1] & 0xff;
            int data = 0;
            if (tag == OBJECT || tag == UNINIT) {
                data = ByteArray.readU16bit(info, pos + 2);
                objectOrUninitialized(tag, data, pos + 2);
                pos += 2;
            }

            sameLocals(top, offset, tag, data);
            return pos + 2;
        }


        public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData)
            throws BadBytecode {}


        public void chopFrame(int pos, int offsetDelta, int k) throws BadBytecode {}

        private int appendFrame(int pos, int type) throws BadBytecode {
            int k = type - 251;
            int offset = ByteArray.readU16bit(info, pos + 1);
            int[] tags = new int[k];
            int[] data = new int[k];
            int p = pos + 3;
            for (int i = 0; i < k; i++) {
                int tag = info[p] & 0xff;
                tags[i] = tag;
                if (tag == OBJECT || tag == UNINIT) {
                    data[i] = ByteArray.readU16bit(info, p + 1);
                    objectOrUninitialized(tag, data[i], p + 1);
                    p += 3;
                }
                else {
                    data[i] = 0;
                    p++;
                }
            }

            appendFrame(pos, offset, tags, data);
            return p;
        }


        public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data)
            throws BadBytecode {} 

        private int fullFrame(int pos) throws BadBytecode {
            int offset = ByteArray.readU16bit(info, pos + 1);
            int numOfLocals = ByteArray.readU16bit(info, pos + 3);
            int[] localsTags = new int[numOfLocals];
            int[] localsData = new int[numOfLocals];
            int p = verifyTypeInfo(pos + 5, numOfLocals, localsTags, localsData);
            int numOfItems = ByteArray.readU16bit(info, p);
            int[] itemsTags = new int[numOfItems];
            int[] itemsData = new int[numOfItems];
            p = verifyTypeInfo(p + 2, numOfItems, itemsTags, itemsData);
            fullFrame(pos, offset, localsTags, localsData, itemsTags, itemsData);
            return p;
        }


        public void fullFrame(int pos, int offsetDelta, int[] localTags,
                int[] localData, int[] stackTags, int[] stackData)
            throws BadBytecode {}

        private int verifyTypeInfo(int pos, int n, int[] tags, int[] data) {
            for (int i = 0; i < n; i++) {
                int tag = info[pos++] & 0xff;
                tags[i] = tag;
                if (tag == OBJECT || tag == UNINIT) {
                    data[i] = ByteArray.readU16bit(info, pos);
                    objectOrUninitialized(tag, data[i], pos);
                    pos += 2;
                }
            }

            return pos;
        }


        public void objectOrUninitialized(int tag, int data, int pos) {}
    }

    static class SimpleCopy extends Walker {
        private Writer writer;

        public SimpleCopy(byte[] data) {
            super(data);
            writer = new Writer(data.length);
        }

        public byte[] doit() throws BadBytecode {
            parse();
            return writer.toByteArray();
        }

        @Override
        public void sameFrame(int pos, int offsetDelta) {
            writer.sameFrame(offsetDelta);
        }

        @Override
        public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData) {
            writer.sameLocals(offsetDelta, stackTag, copyData(stackTag, stackData));
        }

        @Override
        public void chopFrame(int pos, int offsetDelta, int k) {
            writer.chopFrame(offsetDelta, k);
        }

        @Override
        public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data) {
            writer.appendFrame(offsetDelta, tags, copyData(tags, data));
        }

        @Override
        public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData,
                              int[] stackTags, int[] stackData) {
            writer.fullFrame(offsetDelta, localTags, copyData(localTags, localData),
                             stackTags, copyData(stackTags, stackData));
        }

        protected int copyData(int tag, int data) {
            return data;
        }

        protected int[] copyData(int[] tags, int[] data) {
            return data;
        }
    }

    static class Copier extends SimpleCopy {
        private ConstPool srcPool, destPool;
        private Map<String,String> classnames;

        public Copier(ConstPool src, byte[] data, ConstPool dest, Map<String,String> names) {
            super(data);
            srcPool = src;
            destPool = dest;
            classnames = names;
        }

        @Override
        protected int copyData(int tag, int data) {
            if (tag == OBJECT)
                return srcPool.copy(data, destPool, classnames);
            return data;
        }

        @Override
        protected int[] copyData(int[] tags, int[] data) {
            int[] newData = new int[data.length];
            for (int i = 0; i < data.length; i++)
                if (tags[i] == OBJECT)
                    newData[i] = srcPool.copy(data[i], destPool, classnames);
                else
                    newData[i] = data[i];

            return newData;
        }
    }


    public void insertLocal(int index, int tag, int classInfo)
        throws BadBytecode
    {
        byte[] data = new InsertLocal(this.get(), index, tag, classInfo).doit();
        this.set(data);
    }


    public static int typeTagOf(char descriptor) {
        switch (descriptor) {
        case 'D' :
            return DOUBLE;
        case 'F' :
            return FLOAT;
        case 'J' :
            return LONG;
        case 'L' :
        case '[' :
            return OBJECT;

        default :
            return INTEGER;
        }
    }


    static class InsertLocal extends SimpleCopy {
        private int varIndex;
        private int varTag, varData;

        public InsertLocal(byte[] data, int varIndex, int varTag, int varData) {
            super(data);
            this.varIndex = varIndex;
            this.varTag = varTag;
            this.varData = varData;
        }

        @Override
        public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData,
                              int[] stackTags, int[] stackData) {
            int len = localTags.length;
            if (len < varIndex) {
                super.fullFrame(pos, offsetDelta, localTags, localData, stackTags, stackData);
                return;
            }

            int typeSize = (varTag == LONG || varTag == DOUBLE) ? 2 : 1;
            int[] localTags2 = new int[len + typeSize];
            int[] localData2 = new int[len + typeSize];
            int index = varIndex;
            int j = 0;
            for (int i = 0; i < len; i++) {
                if (j == index)
                    j += typeSize;

                localTags2[j] = localTags[i];
                localData2[j++] = localData[i];
            }

            localTags2[index] = varTag;
            localData2[index] = varData;
            if (typeSize > 1) {
                localTags2[index + 1] = TOP;
                localData2[index + 1] = 0;
            }

            super.fullFrame(pos, offsetDelta, localTags2, localData2, stackTags, stackData);
        }
    }


    public static class Writer {
        ByteArrayOutputStream output;
        int numOfEntries;


        public Writer(int size) {
            output = new ByteArrayOutputStream(size);
            numOfEntries = 0;
            output.write(0);
            output.write(0);
        }


        public byte[] toByteArray() {
            byte[] b = output.toByteArray();
            ByteArray.write16bit(numOfEntries, b, 0);
            return b;
        }


        public StackMapTable toStackMapTable(ConstPool cp) {
            return new StackMapTable(cp, toByteArray());
        }


        public void sameFrame(int offsetDelta) {
            numOfEntries++;
            if (offsetDelta < 64)
                output.write(offsetDelta);
            else {
                output.write(251);
                write16(offsetDelta);
            }
        }


        public void sameLocals(int offsetDelta, int tag, int data) {
            numOfEntries++;
            if (offsetDelta < 64)
                output.write(offsetDelta + 64);
            else {
                output.write(247);
                write16(offsetDelta);
            }

            writeTypeInfo(tag, data);
        }


        public void chopFrame(int offsetDelta, int k) {
            numOfEntries++;
            output.write(251 - k);
            write16(offsetDelta);
        }


        public void appendFrame(int offsetDelta, int[] tags, int[] data) {
            numOfEntries++;
            int k = tags.length;
            output.write(k + 251);
            write16(offsetDelta);
            for (int i = 0; i < k; i++)
                writeTypeInfo(tags[i], data[i]);
        }


        public void fullFrame(int offsetDelta, int[] localTags, int[] localData,
                              int[] stackTags, int[] stackData) {
            numOfEntries++;
            output.write(255);
            write16(offsetDelta);
            int n = localTags.length;
            write16(n);
            for (int i = 0; i < n; i++)
                writeTypeInfo(localTags[i], localData[i]);

            n = stackTags.length;
            write16(n);
            for (int i = 0; i < n; i++)
                writeTypeInfo(stackTags[i], stackData[i]);
        }

        private void writeTypeInfo(int tag, int data) {
            output.write(tag);
            if (tag == OBJECT || tag == UNINIT)
                write16(data);
        }

        private void write16(int value) {
            output.write((value >>> 8) & 0xff);
            output.write(value & 0xff);
        }
    }


    public void println(PrintWriter w) {
        Printer.print(this, w);
    }


    public void println(java.io.PrintStream ps) {
        Printer.print(this, new java.io.PrintWriter(ps, true));
    }

    static class Printer extends Walker {
        private PrintWriter writer;
        private int offset;


        public static void print(StackMapTable smt, PrintWriter writer) {
            try {
                new Printer(smt.get(), writer).parse();
            }
            catch (BadBytecode e) {
                writer.println(e.getMessage());
            }
        }

        Printer(byte[] data, PrintWriter pw) {
            super(data);
            writer = pw;
            offset = -1;
        }

        @Override
        public void sameFrame(int pos, int offsetDelta) {
            offset += offsetDelta + 1;
            writer.println(offset + " same frame: " + offsetDelta);
        }

        @Override
        public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData) {
            offset += offsetDelta + 1;
            writer.println(offset + " same locals: " + offsetDelta);
            printTypeInfo(stackTag, stackData);
        }

        @Override
        public void chopFrame(int pos, int offsetDelta, int k) {
            offset += offsetDelta + 1;
            writer.println(offset + " chop frame: " + offsetDelta + ",    " + k + " last locals");
        }

        @Override
        public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data) {
            offset += offsetDelta + 1;
            writer.println(offset + " append frame: " + offsetDelta);
            for (int i = 0; i < tags.length; i++)
                printTypeInfo(tags[i], data[i]);
        }

        @Override
        public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData,
                              int[] stackTags, int[] stackData) {
            offset += offsetDelta + 1;
            writer.println(offset + " full frame: " + offsetDelta);
            writer.println("[locals]");
            for (int i = 0; i < localTags.length; i++)
                printTypeInfo(localTags[i], localData[i]);

            writer.println("[stack]");
            for (int i = 0; i < stackTags.length; i++)
                printTypeInfo(stackTags[i], stackData[i]);
        }

        private void printTypeInfo(int tag, int data) {
            String msg = null;
            switch (tag) {
            case TOP :
                msg = "top";
                break;
            case INTEGER :
                msg = "integer";
                break;
            case FLOAT :
                msg = "float";
                break;
            case DOUBLE :
                msg = "double";
                break;
            case LONG :
                msg = "long";
                break;
            case NULL :
                msg = "null";
                break;
            case THIS :
                msg = "this";
                break;
            case OBJECT :
                msg = "object (cpool_index " + data + ")";
                break;
            case UNINIT :
                msg = "uninitialized (offset " + data + ")";
                break;
            }

            writer.print("    ");
            writer.println(msg);
        }
    }

    void shiftPc(int where, int gapSize, boolean exclusive)
        throws BadBytecode
    {
    	new OffsetShifter(this, where, gapSize).parse();
        new Shifter(this, where, gapSize, exclusive).doit();
    }

    static class OffsetShifter extends Walker {
    	int where, gap;

    	public OffsetShifter(StackMapTable smt, int where, int gap) {
    		super(smt);
    		this.where = where;
    		this.gap = gap;
    	}

    	@Override
        public void objectOrUninitialized(int tag, int data, int pos) {
    		if (tag == UNINIT)
    			if (where <= data)
    				ByteArray.write16bit(data + gap, info, pos);
    	}
    }

    static class Shifter extends Walker {
        private StackMapTable stackMap;
        int where, gap;
        int position;
        byte[] updatedInfo;
        boolean exclusive;

        public Shifter(StackMapTable smt, int where, int gap, boolean exclusive) {
            super(smt);
            stackMap = smt;
            this.where = where;
            this.gap = gap;
            this.position = 0;
            this.updatedInfo = null;
            this.exclusive = exclusive;
        }

        public void doit() throws BadBytecode {
            parse();
            if (updatedInfo != null)
                stackMap.set(updatedInfo);
        }

        @Override
        public void sameFrame(int pos, int offsetDelta) {
            update(pos, offsetDelta, 0, 251);
        }

        @Override
        public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData) {
            update(pos, offsetDelta, 64, 247);
        }

        void update(int pos, int offsetDelta, int base, int entry) {
            int oldPos = position;
            position = oldPos + offsetDelta + (oldPos == 0 ? 0 : 1);
            boolean match;
            if (exclusive)
                match = oldPos < where  && where <= position;
            else
                match = oldPos <= where  && where < position;

            if (match) {
                int newDelta = offsetDelta + gap;
                position += gap;
                if (newDelta < 64)
                    info[pos] = (byte)(newDelta + base);
                else if (offsetDelta < 64) {
                    byte[] newinfo = insertGap(info, pos, 2);
                    newinfo[pos] = (byte)entry;
                    ByteArray.write16bit(newDelta, newinfo, pos + 1);
                    updatedInfo = newinfo;
                }
                else
                    ByteArray.write16bit(newDelta, info, pos + 1);
            }
        }

        static byte[] insertGap(byte[] info, int where, int gap) {
            int len = info.length;
            byte[] newinfo = new byte[len + gap];
            for (int i = 0; i < len; i++)
                newinfo[i + (i < where ? 0 : gap)] = info[i];

            return newinfo;
        }

        @Override
        public void chopFrame(int pos, int offsetDelta, int k) {
            update(pos, offsetDelta);
        }

        @Override
        public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data) {
            update(pos, offsetDelta);
        }

        @Override
        public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData,
                              int[] stackTags, int[] stackData) {
            update(pos, offsetDelta);
        }

        void update(int pos, int offsetDelta) {
            int oldPos = position;
            position = oldPos + offsetDelta + (oldPos == 0 ? 0 : 1);
            boolean match;
            if (exclusive)
                match = oldPos < where  && where <= position;
            else
                match = oldPos <= where  && where < position;

            if (match) {
                int newDelta = offsetDelta + gap;
                ByteArray.write16bit(newDelta, info, pos + 1);
                position += gap;
            }
        }
    }


    void shiftForSwitch(int where, int gapSize) throws BadBytecode {
        new SwitchShifter(this, where, gapSize).doit();
    }

    static class SwitchShifter extends Shifter {
        SwitchShifter(StackMapTable smt, int where, int gap) {
            super(smt, where, gap, false);
        }

        @Override
        void update(int pos, int offsetDelta, int base, int entry) {
            int oldPos = position;
            position = oldPos + offsetDelta + (oldPos == 0 ? 0 : 1);
            int newDelta = offsetDelta;
            if (where == position)
                newDelta = offsetDelta - gap;
            else if (where == oldPos)
                newDelta = offsetDelta + gap;
            else
                return;

            if (offsetDelta < 64)
                if (newDelta < 64)
                    info[pos] = (byte)(newDelta + base);
                else {
                    byte[] newinfo = insertGap(info, pos, 2);
                    newinfo[pos] = (byte)entry;
                    ByteArray.write16bit(newDelta, newinfo, pos + 1);
                    updatedInfo = newinfo;
                }
            else
                if (newDelta < 64) {
                    byte[] newinfo = deleteGap(info, pos, 2);
                    newinfo[pos] = (byte)(newDelta + base);
                    updatedInfo = newinfo;
                }
                else
                    ByteArray.write16bit(newDelta, info, pos + 1);
        }

        static byte[] deleteGap(byte[] info, int where, int gap) {
            where += gap;
            int len = info.length;
            byte[] newinfo = new byte[len - gap];
            for (int i = 0; i < len; i++)
                newinfo[i - (i < where ? 0 : gap)] = info[i];

            return newinfo;
        }

        @Override
        void update(int pos, int offsetDelta) {
            int oldPos = position;
            position = oldPos + offsetDelta + (oldPos == 0 ? 0 : 1);
            int newDelta = offsetDelta;
            if (where == position)
                newDelta = offsetDelta - gap;
            else if (where == oldPos)
                newDelta = offsetDelta + gap;
            else
                return;

            ByteArray.write16bit(newDelta, info, pos + 1);
        }
    }


     public void removeNew(int where) throws CannotCompileException {
        try {
            byte[] data = new NewRemover(this.get(), where).doit();
            this.set(data);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException("bad stack map table", e);
        }
    }

    static class NewRemover extends SimpleCopy {
        int posOfNew;

        public NewRemover(byte[] data, int pos) {
            super(data);
            posOfNew = pos;
        }

        @Override
        public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData) {
            if (stackTag == UNINIT && stackData == posOfNew)
                super.sameFrame(pos, offsetDelta);
            else
                super.sameLocals(pos, offsetDelta, stackTag, stackData);
        }

        @Override
        public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData,
                              int[] stackTags, int[] stackData) {
            int n = stackTags.length - 1;
            for (int i = 0; i < n; i++)
                if (stackTags[i] == UNINIT && stackData[i] == posOfNew
                    && stackTags[i + 1] == UNINIT && stackData[i + 1] == posOfNew) {
                    n++;
                    int[] stackTags2 = new int[n - 2];
                    int[] stackData2 = new int[n - 2];
                    int k = 0;
                    for (int j = 0; j < n; j++)
                        if (j == i)
                            j++;
                        else {
                            stackTags2[k] = stackTags[j];
                            stackData2[k++] = stackData[j];
                        }

                    stackTags = stackTags2;
                    stackData = stackData2;
                    break;
                }

            super.fullFrame(pos, offsetDelta, localTags, localData, stackTags, stackData);
        }
    }
}
