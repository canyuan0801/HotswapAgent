

package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.ConstPool;


public class DoubleMemberValue extends MemberValue {
    int valueIndex;


    public DoubleMemberValue(int index, ConstPool cp) {
        super('D', cp);
        this.valueIndex = index;
    }


    public DoubleMemberValue(double d, ConstPool cp) {
        super('D', cp);
        setValue(d);
    }


    public DoubleMemberValue(ConstPool cp) {
        super('D', cp);
        setValue(0.0);
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return Double.valueOf(getValue());
    }

    @Override
    Class<?> getType(ClassLoader cl) {
        return double.class;
    }


    public double getValue() {
        return cp.getDoubleInfo(valueIndex);
    }


    public void setValue(double newValue) {
        valueIndex = cp.addDoubleInfo(newValue);
    }


    @Override
    public String toString() {
        return Double.toString(getValue());
    }


    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.constValueIndex(getValue());
    }


    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitDoubleMemberValue(this);
    }
}
