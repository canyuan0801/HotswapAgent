

package org.hotswap.agent.javassist.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ExceptionTableEntry {
    int startPc;
    int endPc;
    int handlerPc;
    int catchType;

    ExceptionTableEntry(int start, int end, int handle, int type) {
        startPc = start;
        endPc = end;
        handlerPc = handle;
        catchType = type;
    }
}


public class ExceptionTable implements Cloneable {
    private ConstPool constPool;
    private List<ExceptionTableEntry> entries;


    public ExceptionTable(ConstPool cp) {
        constPool = cp;
        entries = new ArrayList<ExceptionTableEntry>();
    }

    ExceptionTable(ConstPool cp, DataInputStream in) throws IOException {
        constPool = cp;
        int length = in.readUnsignedShort();
        List<ExceptionTableEntry> list = new ArrayList<ExceptionTableEntry>(length);
        for (int i = 0; i < length; ++i) {
            int start = in.readUnsignedShort();
            int end = in.readUnsignedShort();
            int handle = in.readUnsignedShort();
            int type = in.readUnsignedShort();
            list.add(new ExceptionTableEntry(start, end, handle, type));
        }

        entries = list;
    }


    @Override
    public Object clone() throws CloneNotSupportedException {
        ExceptionTable r = (ExceptionTable)super.clone();
        r.entries = new ArrayList<ExceptionTableEntry>(entries);
        return r;
    }


    public int size() {
        return entries.size();
    }


    public int startPc(int nth) {
        return entries.get(nth).startPc;
    }


    public void setStartPc(int nth, int value) {
        entries.get(nth).startPc = value;
    }


    public int endPc(int nth) {
        return entries.get(nth).endPc;
    }


    public void setEndPc(int nth, int value) {
        entries.get(nth).endPc = value;
    }


    public int handlerPc(int nth) {
        return entries.get(nth).handlerPc;
    }


    public void setHandlerPc(int nth, int value) {
        entries.get(nth).handlerPc = value;
    }


    public int catchType(int nth) {
        return entries.get(nth).catchType;
    }


    public void setCatchType(int nth, int value) {
        entries.get(nth).catchType = value;
    }


    public void add(int index, ExceptionTable table, int offset) {
        int len = table.size();
        while (--len >= 0) {
            ExceptionTableEntry e = table.entries.get(len);
            add(index, e.startPc + offset, e.endPc + offset,
                e.handlerPc + offset, e.catchType);
        }
    }


    public void add(int index, int start, int end, int handler, int type) {
        if (start < end)
            entries.add(index,
                    new ExceptionTableEntry(start, end, handler, type));
    }


    public void add(int start, int end, int handler, int type) {
        if (start < end)
            entries.add(new ExceptionTableEntry(start, end, handler, type));
    }


    public void remove(int index) {
        entries.remove(index);
    }


    public ExceptionTable copy(ConstPool newCp, Map<String,String> classnames) {
        ExceptionTable et = new ExceptionTable(newCp);
        ConstPool srcCp = constPool;
        for (ExceptionTableEntry e:entries) {
            int type = srcCp.copy(e.catchType, newCp, classnames);
            et.add(e.startPc, e.endPc, e.handlerPc, type);
        }

        return et;
    }

    void shiftPc(int where, int gapLength, boolean exclusive) {
        for (ExceptionTableEntry e:entries) {
            e.startPc = shiftPc(e.startPc, where, gapLength, exclusive);
            e.endPc = shiftPc(e.endPc, where, gapLength, exclusive);
            e.handlerPc = shiftPc(e.handlerPc, where, gapLength, exclusive);
        }
    }

    private static int shiftPc(int pc, int where, int gapLength,
                               boolean exclusive) {
        if (pc > where || (exclusive && pc == where))
            pc += gapLength;

        return pc;
    }

    void write(DataOutputStream out) throws IOException {
        out.writeShort(size());
        for (ExceptionTableEntry e:entries) {
            out.writeShort(e.startPc);
            out.writeShort(e.endPc);
            out.writeShort(e.handlerPc);
            out.writeShort(e.catchType);
        }
    }
}
