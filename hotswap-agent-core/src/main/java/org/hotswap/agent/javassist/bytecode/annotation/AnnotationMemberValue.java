
package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.ConstPool;


public class AnnotationMemberValue extends MemberValue {
    Annotation value;


    public AnnotationMemberValue(ConstPool cp) {
        this(null, cp);
    }


    public AnnotationMemberValue(Annotation a, ConstPool cp) {
        super('@', cp);
        value = a;
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m)
        throws ClassNotFoundException
    {
        return AnnotationImpl.make(cl, getType(cl), cp, value);
    }

    @Override
    Class<?> getType(ClassLoader cl) throws ClassNotFoundException {
        if (value == null)
            throw new ClassNotFoundException("no type specified");
        return loadClass(cl, value.getTypeName());
    }


    public Annotation getValue() {
        return value;
    }


    public void setValue(Annotation newValue) {
        value = newValue;
    }


    @Override
    public String toString() {
        return value.toString();
    }


    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.annotationValue();
        value.write(writer);
    }


    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitAnnotationMemberValue(this);
    }
}
