

package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.ConstPool;


public class IntegerMemberValue extends MemberValue {
    int valueIndex;


    public IntegerMemberValue(int index, ConstPool cp) {
        super('I', cp);
        this.valueIndex = index;
    }


    public IntegerMemberValue(ConstPool cp, int value) {
        super('I', cp);
        setValue(value);
    }


    public IntegerMemberValue(ConstPool cp) {
        super('I', cp);
        setValue(0);
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return Integer.valueOf(getValue());
    }

    @Override
    Class<?> getType(ClassLoader cl) {
        return int.class;
    }


    public int getValue() {
        return cp.getIntegerInfo(valueIndex);
    }


    public void setValue(int newValue) {
        valueIndex = cp.addIntegerInfo(newValue);
    }


    @Override
    public String toString() {
        return Integer.toString(getValue());
    }


    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.constValueIndex(getValue());
    }


    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitIntegerMemberValue(this);
    }
}
