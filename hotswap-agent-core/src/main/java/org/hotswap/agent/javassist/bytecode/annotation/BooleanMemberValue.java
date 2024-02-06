
package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.ConstPool;


public class BooleanMemberValue extends MemberValue {
    int valueIndex;


    public BooleanMemberValue(int index, ConstPool cp) {
        super('Z', cp);
        this.valueIndex = index;
    }


    public BooleanMemberValue(boolean b, ConstPool cp) {
        super('Z', cp);
        setValue(b);
    }


    public BooleanMemberValue(ConstPool cp) {
        super('Z', cp);
        setValue(false);
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return Boolean.valueOf(getValue());
    }

    @Override
    Class<?> getType(ClassLoader cl) {
        return boolean.class;
    }


    public boolean getValue() {
        return cp.getIntegerInfo(valueIndex) != 0;
    }


    public void setValue(boolean newValue) {
        valueIndex = cp.addIntegerInfo(newValue ? 1 : 0);
    }


    @Override
    public String toString() {
        return getValue() ? "true" : "false";
    }


    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.constValueIndex(getValue());
    }


    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitBooleanMemberValue(this);
    }
}
