

package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.ConstPool;


public class StringMemberValue extends MemberValue {
    int valueIndex;

    
    public StringMemberValue(int index, ConstPool cp) {
        super('s', cp);
        this.valueIndex = index;
    }

    
    public StringMemberValue(String str, ConstPool cp) {
        super('s', cp);
        setValue(str);
    }

    
    public StringMemberValue(ConstPool cp) {
        super('s', cp);
        setValue("");
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return getValue();
    }

    @Override
    Class<?> getType(ClassLoader cl) {
        return String.class;
    }

    
    public String getValue() {
        return cp.getUtf8Info(valueIndex);
    }

    
    public void setValue(String newValue) {
        valueIndex = cp.addUtf8Info(newValue);
    }

    
    @Override
    public String toString() {
        return "\"" + getValue() + "\"";
    }

    
    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.constValueIndex(getValue());
    }

    
    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitStringMemberValue(this);
    }
}
