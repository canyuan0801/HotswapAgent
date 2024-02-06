

package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.Descriptor;


public class EnumMemberValue extends MemberValue {
    int typeIndex, valueIndex;


    public EnumMemberValue(int type, int value, ConstPool cp) {
        super('e', cp);
        this.typeIndex = type;
        this.valueIndex = value;
    }


    public EnumMemberValue(ConstPool cp) {
        super('e', cp);
        typeIndex = valueIndex = 0;
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m)
        throws ClassNotFoundException
    {
        try {
            return getType(cl).getField(getValue()).get(null);
        }
        catch (NoSuchFieldException e) {
            throw new ClassNotFoundException(getType() + "." + getValue());
        }
        catch (IllegalAccessException e) {
            throw new ClassNotFoundException(getType() + "." + getValue());
        }
    }

    @Override
    Class<?> getType(ClassLoader cl) throws ClassNotFoundException {
        return loadClass(cl, getType());
    }


    public String getType() {
        return Descriptor.toClassName(cp.getUtf8Info(typeIndex));
    }


    public void setType(String typename) {
        typeIndex = cp.addUtf8Info(Descriptor.of(typename));
    }


    public String getValue() {
        return cp.getUtf8Info(valueIndex);
    }


    public void setValue(String name) {
        valueIndex = cp.addUtf8Info(name);
    }

    @Override
    public String toString() {
        return getType() + "." + getValue();
    }


    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.enumConstValue(cp.getUtf8Info(typeIndex), getValue());
    }


    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitEnumMemberValue(this);
    }
}
