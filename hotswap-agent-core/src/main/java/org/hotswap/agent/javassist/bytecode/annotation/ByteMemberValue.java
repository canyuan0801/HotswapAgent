
package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.ConstPool;


public class ByteMemberValue extends MemberValue {
    int valueIndex;


    public ByteMemberValue(int index, ConstPool cp) {
        super('B', cp);
        this.valueIndex = index;
    }


    public ByteMemberValue(byte b, ConstPool cp) {
        super('B', cp);
        setValue(b);
    }


    public ByteMemberValue(ConstPool cp) {
        super('B', cp);
        setValue((byte)0);
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return Byte.valueOf(getValue());
    }

    @Override
    Class<?> getType(ClassLoader cl) {
        return byte.class;
    }


    public byte getValue() {
        return (byte)cp.getIntegerInfo(valueIndex);
    }


    public void setValue(byte newValue) {
        valueIndex = cp.addIntegerInfo(newValue);
    }


    @Override
    public String toString() {
        return Byte.toString(getValue());
    }


    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.constValueIndex(getValue());
    }


    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitByteMemberValue(this);
    }
}
