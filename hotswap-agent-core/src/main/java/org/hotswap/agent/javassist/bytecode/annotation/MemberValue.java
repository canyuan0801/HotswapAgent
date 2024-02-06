

package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.Descriptor;


public abstract class MemberValue {
    ConstPool cp;
    char tag;

    MemberValue(char tag, ConstPool cp) {
        this.cp = cp;
        this.tag = tag;
    }


    abstract Object getValue(ClassLoader cl, ClassPool cp, Method m)
        throws ClassNotFoundException;

    abstract Class<?> getType(ClassLoader cl) throws ClassNotFoundException;

    static Class<?> loadClass(ClassLoader cl, String classname)
        throws ClassNotFoundException, NoSuchClassError
    {
        try {
            return Class.forName(convertFromArray(classname), true, cl);
        }
        catch (LinkageError e) {
            throw new NoSuchClassError(classname, e);
        }
    }

    private static String convertFromArray(String classname)
    {
        int index = classname.indexOf("[]");
        if (index != -1) {
            String rawType = classname.substring(0, index);
            StringBuffer sb = new StringBuffer(Descriptor.of(rawType));
            while (index != -1) {
                sb.insert(0, "[");
                index = classname.indexOf("[]", index + 1);
            }
            return sb.toString().replace('/', '.');
        }
        return classname;
    }


    public abstract void accept(MemberValueVisitor visitor);


    public abstract void write(AnnotationsWriter w) throws IOException;
}


