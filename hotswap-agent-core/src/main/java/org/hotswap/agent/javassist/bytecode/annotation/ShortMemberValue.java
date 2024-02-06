

package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.ConstPool;


public class ShortMemberValue extends MemberValue {
    int valueIndex;


    public ShortMemberValue(int index, ConstPool cp) {
        super('S', cp);
        this.valueIndex = index;
    }


    public ShortMemberValue(short s, ConstPool cp) {
        super('S', cp);
        setValue(s);
    }


    public ShortMemberValue(ConstPool cp) {
        super('S', cp);
        setValue((short)0);
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return Short.valueOf(getValue());
    }

    @Override
    Class<?> getType(ClassLoader cl) {
        return short.class;
    }


    public short getValue() {
        return (short)cp.getIntegerInfo(valueIndex);
    }


    public void setValue(short newValue) {
        valueIndex = cp.addIntegerInfo(newValue);
    }


    @Override
    public String toString() {
        return Short.toString(getValue());
    }


    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.constValueIndex(getValue());
    }


    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitShortMemberValue(this);
    }
}
