

package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.ConstPool;


public class CharMemberValue extends MemberValue {
    int valueIndex;


    public CharMemberValue(int index, ConstPool cp) {
        super('C', cp);
        this.valueIndex = index;
    }


    public CharMemberValue(char c, ConstPool cp) {
        super('C', cp);
        setValue(c);
    }


    public CharMemberValue(ConstPool cp) {
        super('C', cp);
        setValue('\0');
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return Character.valueOf(getValue());
    }

    @Override
    Class<?> getType(ClassLoader cl) {
        return char.class;
    }


    public char getValue() {
        return (char)cp.getIntegerInfo(valueIndex);
    }


    public void setValue(char newValue) {
        valueIndex = cp.addIntegerInfo(newValue);
    }


    @Override
    public String toString() {
        return Character.toString(getValue());
    }


    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.constValueIndex(getValue());
    }


    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitCharMemberValue(this);
    }
}
