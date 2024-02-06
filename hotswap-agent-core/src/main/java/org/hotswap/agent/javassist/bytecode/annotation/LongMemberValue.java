

package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.ConstPool;


public class LongMemberValue extends MemberValue {
    int valueIndex;


    public LongMemberValue(int index, ConstPool cp) {
        super('J', cp);
        this.valueIndex = index;
    }


    public LongMemberValue(long j, ConstPool cp) {
        super('J', cp);
        setValue(j);
    }


    public LongMemberValue(ConstPool cp) {
        super('J', cp);
        setValue(0L);
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return Long.valueOf(getValue());
    }

    @Override
    Class<?> getType(ClassLoader cl) {
        return long.class;
    }


    public long getValue() {
        return cp.getLongInfo(valueIndex);
    }


    public void setValue(long newValue) {
        valueIndex = cp.addLongInfo(newValue);
    }


    @Override
    public String toString() {
        return Long.toString(getValue());
    }


    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.constValueIndex(getValue());
    }


    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitLongMemberValue(this);
    }
}
