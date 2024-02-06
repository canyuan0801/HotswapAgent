

package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.Descriptor;
import org.hotswap.agent.javassist.bytecode.SignatureAttribute;


public class ClassMemberValue extends MemberValue {
    int valueIndex;


    public ClassMemberValue(int index, ConstPool cp) {
        super('c', cp);
        this.valueIndex = index;
    }


    public ClassMemberValue(String className, ConstPool cp) {
        super('c', cp);
        setValue(className);
    }


    public ClassMemberValue(ConstPool cp) {
        super('c', cp);
        setValue("java.lang.Class");
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m)
            throws ClassNotFoundException {
        final String classname = getValue();
        if (classname.equals("void"))
            return void.class;
        else if (classname.equals("int"))
            return int.class;
        else if (classname.equals("byte"))
            return byte.class;
        else if (classname.equals("long"))
            return long.class;
        else if (classname.equals("double"))
            return double.class;
        else if (classname.equals("float"))
            return float.class;
        else if (classname.equals("char"))
            return char.class;
        else if (classname.equals("short"))
            return short.class;
        else if (classname.equals("boolean"))
            return boolean.class;
        else
            return loadClass(cl, classname);
    }

    @Override
    Class<?> getType(ClassLoader cl) throws ClassNotFoundException {
        return loadClass(cl, "java.lang.Class");
    }


    public String getValue() {
        String v = cp.getUtf8Info(valueIndex);
        try {
            return SignatureAttribute.toTypeSignature(v).jvmTypeName();
        } catch (BadBytecode e) {
            throw new RuntimeException(e);
        }
    }


    public void setValue(String newClassName) {
        String setTo = Descriptor.of(newClassName);
        valueIndex = cp.addUtf8Info(setTo);
    }


    @Override
    public String toString() {
        return getValue().replace('$', '.') + ".class";
    }


    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.classInfoIndex(cp.getUtf8Info(valueIndex));
    }


    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitClassMemberValue(this);
    }
}
