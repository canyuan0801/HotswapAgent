
package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.ConstPool;


public class ArrayMemberValue extends MemberValue {
    MemberValue type;
    MemberValue[] values;


    public ArrayMemberValue(ConstPool cp) {
        super('[', cp);
        type = null;
        values = null;
    }


    public ArrayMemberValue(MemberValue t, ConstPool cp) {
        super('[', cp);
        type = t;
        values = null;
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method method)
        throws ClassNotFoundException
    {
        if (values == null)
            throw new ClassNotFoundException(
                        "no array elements found: " + method.getName());

        int size = values.length;
        Class<?> clazz;
        if (type == null) {
            clazz = method.getReturnType().getComponentType();
            if (clazz == null || size > 0)
                throw new ClassNotFoundException("broken array type: "
                                                 + method.getName());
        }
        else
            clazz = type.getType(cl);

        Object a = Array.newInstance(clazz, size);
        for (int i = 0; i < size; i++)
            Array.set(a, i, values[i].getValue(cl, cp, method));

        return a;
    }

    @Override
    Class<?> getType(ClassLoader cl) throws ClassNotFoundException {
        if (type == null)
            throw new ClassNotFoundException("no array type specified");

        Object a = Array.newInstance(type.getType(cl), 0);
        return a.getClass();
    }


    public MemberValue getType() {
        return type;
    }


    public MemberValue[] getValue() {
        return values;
    }


    public void setValue(MemberValue[] elements) {
        values = elements;
        if (elements != null && elements.length > 0)
            type = elements[0];
    }


    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("{");
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                buf.append(values[i].toString());
                if (i + 1 < values.length)
                    buf.append(", ");
                }
        }

        buf.append("}");
        return buf.toString();
    }


    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        int num = values == null ? 0 : values.length;
        writer.arrayValue(num);
        for (int i = 0; i < num; ++i)
            values[i].write(writer);
    }


    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitArrayMemberValue(this);
    }
}
