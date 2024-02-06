

package org.hotswap.agent.javassist.bytecode.annotation;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.Descriptor;


public class Annotation {
    static class Pair {
        int name;
        MemberValue value;
    }

    ConstPool pool;
    int typeIndex;
    Map<String,Pair> members;



    public Annotation(int type, ConstPool cp) {
        pool = cp;
        typeIndex = type;
        members = null;
    }


    public Annotation(String typeName, ConstPool cp) {
        this(cp.addUtf8Info(Descriptor.of(typeName)), cp);
    }


    public Annotation(ConstPool cp, CtClass clazz)
        throws NotFoundException
    {

        this(cp.addUtf8Info(Descriptor.of(clazz.getName())), cp);

        if (!clazz.isInterface())
            throw new RuntimeException(
                "Only interfaces are allowed for Annotation creation.");

        CtMethod[] methods = clazz.getDeclaredMethods();
        if (methods.length > 0)
            members = new LinkedHashMap<String,Pair>();

        for (CtMethod m:methods)
            addMemberValue(m.getName(),
                           createMemberValue(cp, m.getReturnType()));
    }


    public static MemberValue createMemberValue(ConstPool cp, CtClass type)
        throws NotFoundException
    {
        if (type == CtClass.booleanType)
            return new BooleanMemberValue(cp);
        else if (type == CtClass.byteType)
            return new ByteMemberValue(cp);
        else if (type == CtClass.charType)
            return new CharMemberValue(cp);
        else if (type == CtClass.shortType)
            return new ShortMemberValue(cp);
        else if (type == CtClass.intType)
            return new IntegerMemberValue(cp);
        else if (type == CtClass.longType)
            return new LongMemberValue(cp);
        else if (type == CtClass.floatType)
            return new FloatMemberValue(cp);
        else if (type == CtClass.doubleType)
            return new DoubleMemberValue(cp);
        else if (type.getName().equals("java.lang.Class"))
            return new ClassMemberValue(cp);
        else if (type.getName().equals("java.lang.String"))
            return new StringMemberValue(cp);
        else if (type.isArray()) {
            CtClass arrayType = type.getComponentType();
            MemberValue member = createMemberValue(cp, arrayType);
            return new ArrayMemberValue(member, cp);
        }
        else if (type.isInterface()) {
            Annotation info = new Annotation(cp, type);
            return new AnnotationMemberValue(info, cp);
        }
        else {



            EnumMemberValue emv = new EnumMemberValue(cp);
            emv.setType(type.getName());
            return emv;
        }
    }


    public void addMemberValue(int nameIndex, MemberValue value) {
        Pair p = new Pair();
        p.name = nameIndex;
        p.value = value;
        addMemberValue(p);
    }


    public void addMemberValue(String name, MemberValue value) {
        Pair p = new Pair();
        p.name = pool.addUtf8Info(name);
        p.value = value;
        if (members == null)
            members = new LinkedHashMap<String,Pair>();

        members.put(name, p);
    }

    private void addMemberValue(Pair pair) {
        String name = pool.getUtf8Info(pair.name);
        if (members == null)
            members = new LinkedHashMap<String,Pair>();

        members.put(name, pair);
    }


    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("@");
        buf.append(getTypeName());
        if (members != null) {
            buf.append("(");
            for (String name:members.keySet()) {
                buf.append(name).append("=")
                   .append(getMemberValue(name))
                   .append(", ");
            }
            buf.setLength(buf.length()-2);
            buf.append(")");
        }

        return buf.toString();
    }


    public String getTypeName() {
        return Descriptor.toClassName(pool.getUtf8Info(typeIndex));
    }


    public Set<String> getMemberNames() {
        if (members == null)
            return null;
        return members.keySet();
    }


    public MemberValue getMemberValue(String name) {
        if (members == null||members.get(name) == null)
            return null;
        return members.get(name).value;
    }


    public Object toAnnotationType(ClassLoader cl, ClassPool cp)
        throws ClassNotFoundException, NoSuchClassError
    {
        Class<?> clazz = MemberValue.loadClass(cl, getTypeName());
        try {
            return AnnotationImpl.make(cl, clazz, cp, this);
        }
        catch (IllegalArgumentException e) {

            throw new ClassNotFoundException(clazz.getName(), e);
        }
        catch (IllegalAccessError e2) {

            throw new ClassNotFoundException(clazz.getName(), e2);
        }
    }


    public void write(AnnotationsWriter writer) throws IOException {
        String typeName = pool.getUtf8Info(typeIndex);
        if (members == null) {
            writer.annotation(typeName, 0);
            return;
        }

        writer.annotation(typeName, members.size());
        for (Pair pair:members.values()) {
            writer.memberValuePair(pair.name);
            pair.value.write(writer);
        }
    }

    @Override
    public int hashCode() {
        return getTypeName().hashCode() +
                (members == null ? 0 : members.hashCode());
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj instanceof Annotation == false)
            return false;

        Annotation other = (Annotation) obj;

        if (getTypeName().equals(other.getTypeName()) == false)
            return false;

        Map<String,Pair> otherMembers = other.members;
        if (members == otherMembers)
            return true;
        else if (members == null)
            return otherMembers == null;
        else
            if (otherMembers == null)
                return false;
            else
                return members.equals(otherMembers);
    }
}
