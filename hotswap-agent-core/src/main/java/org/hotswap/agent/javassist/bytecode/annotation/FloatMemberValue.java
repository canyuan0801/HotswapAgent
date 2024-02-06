

package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.ConstPool;


public class FloatMemberValue extends MemberValue {
    int valueIndex;


    public FloatMemberValue(int index, ConstPool cp) {
        super('F', cp);
        this.valueIndex = index;
    }


    public FloatMemberValue(float f, ConstPool cp) {
        super('F', cp);
        setValue(f);
    }


    public FloatMemberValue(ConstPool cp) {
        super('F', cp);
        setValue(0.0F);
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return Float.valueOf(getValue());
    }

    @Override
    Class<?> getType(ClassLoader cl) {
        return float.class;
    }


    public float getValue() {
        return cp.getFloatInfo(valueIndex);
    }


    public void setValue(float newValue) {
        valueIndex = cp.addFloatInfo(newValue);
    }


    @Override
    public String toString() {
        return Float.toString(getValue());
    }


    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.constValueIndex(getValue());
    }


    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitFloatMemberValue(this);
    }
}
