

package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.io.OutputStream;

import org.hotswap.agent.javassist.bytecode.ByteArray;
import org.hotswap.agent.javassist.bytecode.ConstPool;


public class AnnotationsWriter {
    protected OutputStream output;
    private ConstPool pool;


    public AnnotationsWriter(OutputStream os, ConstPool cp) {
        output = os;
        pool = cp;
    }


    public ConstPool getConstPool() {
        return pool;
    }


    public void close() throws IOException {
        output.close();
    }


    public void numParameters(int num) throws IOException {
        output.write(num);
    }


    public void numAnnotations(int num) throws IOException {
        write16bit(num);
    }


    public void annotation(String type, int numMemberValuePairs)
        throws IOException
    {
        annotation(pool.addUtf8Info(type), numMemberValuePairs);
    }


    public void annotation(int typeIndex, int numMemberValuePairs)
        throws IOException
    {
        write16bit(typeIndex);
        write16bit(numMemberValuePairs);
    }


    public void memberValuePair(String memberName) throws IOException {
        memberValuePair(pool.addUtf8Info(memberName));
    }


    public void memberValuePair(int memberNameIndex) throws IOException {
        write16bit(memberNameIndex);
    }


    public void constValueIndex(boolean value) throws IOException {
        constValueIndex('Z', pool.addIntegerInfo(value ? 1 : 0));
    }


    public void constValueIndex(byte value) throws IOException {
        constValueIndex('B', pool.addIntegerInfo(value));
    }


    public void constValueIndex(char value) throws IOException {
        constValueIndex('C', pool.addIntegerInfo(value));
    }


    public void constValueIndex(short value) throws IOException {
        constValueIndex('S', pool.addIntegerInfo(value));
    }


    public void constValueIndex(int value) throws IOException {
        constValueIndex('I', pool.addIntegerInfo(value));
    }


    public void constValueIndex(long value) throws IOException {
        constValueIndex('J', pool.addLongInfo(value));
    }


    public void constValueIndex(float value) throws IOException {
        constValueIndex('F', pool.addFloatInfo(value));
    }


    public void constValueIndex(double value) throws IOException {
        constValueIndex('D', pool.addDoubleInfo(value));
    }


    public void constValueIndex(String value) throws IOException {
        constValueIndex('s', pool.addUtf8Info(value));
    }


    public void constValueIndex(int tag, int index)
        throws IOException
    {
        output.write(tag);
        write16bit(index);
    }


    public void enumConstValue(String typeName, String constName)
        throws IOException
    {
        enumConstValue(pool.addUtf8Info(typeName),
                       pool.addUtf8Info(constName));
    }


    public void enumConstValue(int typeNameIndex, int constNameIndex)
        throws IOException
    {
        output.write('e');
        write16bit(typeNameIndex);
        write16bit(constNameIndex);
    }


    public void classInfoIndex(String name) throws IOException {
        classInfoIndex(pool.addUtf8Info(name));
    }


    public void classInfoIndex(int index) throws IOException {
        output.write('c');
        write16bit(index);
    }


    public void annotationValue() throws IOException {
        output.write('@');
    }


    public void arrayValue(int numValues) throws IOException {
        output.write('[');
        write16bit(numValues);
    }

    protected void write16bit(int value) throws IOException {
        byte[] buf = new byte[2];
        ByteArray.write16bit(value, buf, 0);
        output.write(buf);
    }
}
