

package org.hotswap.agent.javassist;

import java.util.HashMap;

import org.hotswap.agent.javassist.bytecode.Descriptor;


public class ClassMap extends HashMap<String,String> {

    private static final long serialVersionUID = 1L;
    private ClassMap parent;


    public ClassMap() { parent = null; }

    ClassMap(ClassMap map) { parent = map; }


    public void put(CtClass oldname, CtClass newname) {
        put(oldname.getName(), newname.getName());
    }


    @Override
    public String put(String oldname, String newname) {
        if (oldname == newname)
            return oldname;

        String oldname2 = toJvmName(oldname);
        String s = get(oldname2);
        if (s == null || !s.equals(oldname2))
            return super.put(oldname2, toJvmName(newname));
        return s;
    }


    public void putIfNone(String oldname, String newname) {
        if (oldname == newname)
            return;

        String oldname2 = toJvmName(oldname);
        String s = get(oldname2);
        if (s == null)
            super.put(oldname2, toJvmName(newname));
    }

    protected final String put0(String oldname, String newname) {
        return super.put(oldname, newname);
    }


    @Override
    public String get(Object jvmClassName) {
        String found = super.get(jvmClassName);
        if (found == null && parent != null)
            return parent.get(jvmClassName);
        return found;
    }

    public void fix(CtClass clazz) {
        fix(clazz.getName());
    }


    public void fix(String name) {
        String name2 = toJvmName(name);
        super.put(name2, name2);
    }


    public static String toJvmName(String classname) {
        return Descriptor.toJvmName(classname);
    }


    public static String toJavaName(String classname) {
        return Descriptor.toJavaName(classname);
    }
}
