

package org.hotswap.agent.javassist.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.bytecode.stackmap.MapMaker;


public class MethodInfo {
    ConstPool constPool;
    int accessFlags;
    int name;
    String cachedName;
    int descriptor;
    List<AttributeInfo> attribute;


    public static boolean doPreverify = false;


    public static final String nameInit = "<init>";


    public static final String nameClinit = "<clinit>";

    private MethodInfo(ConstPool cp) {
        constPool = cp;
        attribute = null;
    }


    public MethodInfo(ConstPool cp, String methodname, String desc) {
        this(cp);
        accessFlags = 0;
        name = cp.addUtf8Info(methodname);
        cachedName = methodname;
        descriptor = constPool.addUtf8Info(desc);
    }

    MethodInfo(ConstPool cp, DataInputStream in) throws IOException {
        this(cp);
        read(in);
    }


    public MethodInfo(ConstPool cp, String methodname, MethodInfo src,
            Map<String,String> classnameMap) throws BadBytecode
    {
        this(cp);
        read(src, methodname, classnameMap);
    }


    @Override
    public String toString() {
        return getName() + " " + getDescriptor();
    }


    void compact(ConstPool cp) {
        name = cp.addUtf8Info(getName());
        descriptor = cp.addUtf8Info(getDescriptor());
        attribute = AttributeInfo.copyAll(attribute, cp);
        constPool = cp;
    }

    void prune(ConstPool cp) {
        List<AttributeInfo> newAttributes = new ArrayList<AttributeInfo>();

        AttributeInfo invisibleAnnotations
            = getAttribute(AnnotationsAttribute.invisibleTag);
        if (invisibleAnnotations != null) {
            invisibleAnnotations = invisibleAnnotations.copy(cp, null);
            newAttributes.add(invisibleAnnotations);
        }

        AttributeInfo visibleAnnotations
            = getAttribute(AnnotationsAttribute.visibleTag);
        if (visibleAnnotations != null) {
            visibleAnnotations = visibleAnnotations.copy(cp, null);
            newAttributes.add(visibleAnnotations);
        }

        AttributeInfo parameterInvisibleAnnotations
            = getAttribute(ParameterAnnotationsAttribute.invisibleTag);
        if (parameterInvisibleAnnotations != null) {
            parameterInvisibleAnnotations = parameterInvisibleAnnotations.copy(cp, null);
            newAttributes.add(parameterInvisibleAnnotations);
        }

        AttributeInfo parameterVisibleAnnotations
            = getAttribute(ParameterAnnotationsAttribute.visibleTag);
        if (parameterVisibleAnnotations != null) {
            parameterVisibleAnnotations = parameterVisibleAnnotations.copy(cp, null);
            newAttributes.add(parameterVisibleAnnotations);
        }

        AnnotationDefaultAttribute defaultAttribute
             = (AnnotationDefaultAttribute) getAttribute(AnnotationDefaultAttribute.tag);
        if (defaultAttribute != null)
            newAttributes.add(defaultAttribute);

        ExceptionsAttribute ea = getExceptionsAttribute();
        if (ea != null)
            newAttributes.add(ea);

        AttributeInfo signature 
            = getAttribute(SignatureAttribute.tag);
        if (signature != null) {
            signature = signature.copy(cp, null);
            newAttributes.add(signature);
        }
        
        attribute = newAttributes;
        name = cp.addUtf8Info(getName());
        descriptor = cp.addUtf8Info(getDescriptor());
        constPool = cp;
    }


    public String getName() {
       if (cachedName == null)
           cachedName = constPool.getUtf8Info(name);

       return cachedName;
    }


    public void setName(String newName) {
        name = constPool.addUtf8Info(newName);
        cachedName = newName;
    }


    public boolean isMethod() {
        String n = getName();
        return !n.equals(nameInit) && !n.equals(nameClinit);
    }


    public ConstPool getConstPool() {
        return constPool;
    }


    public boolean isConstructor() {
        return getName().equals(nameInit);
    }


    public boolean isStaticInitializer() {
        return getName().equals(nameClinit);
    }


    public int getAccessFlags() {
        return accessFlags;
    }


    public void setAccessFlags(int acc) {
        accessFlags = acc;
    }


    public String getDescriptor() {
        return constPool.getUtf8Info(descriptor);
    }


    public void setDescriptor(String desc) {
        if (!desc.equals(getDescriptor()))
            descriptor = constPool.addUtf8Info(desc);
    }


    public List<AttributeInfo> getAttributes() {
        if (attribute == null)
            attribute = new ArrayList<AttributeInfo>();

        return attribute;
    }


    public AttributeInfo getAttribute(String name) {
        return AttributeInfo.lookup(attribute, name);
    }


    public AttributeInfo removeAttribute(String name) {
        return AttributeInfo.remove(attribute, name);
    }


    public void addAttribute(AttributeInfo info) {
        if (attribute == null)
            attribute = new ArrayList<AttributeInfo>();

        AttributeInfo.remove(attribute, info.getName());
        attribute.add(info);
    }


    public ExceptionsAttribute getExceptionsAttribute() {
        AttributeInfo info = AttributeInfo.lookup(attribute,
                ExceptionsAttribute.tag);
        return (ExceptionsAttribute)info;
    }


    public CodeAttribute getCodeAttribute() {
        AttributeInfo info = AttributeInfo.lookup(attribute, CodeAttribute.tag);
        return (CodeAttribute)info;
    }


    public void removeExceptionsAttribute() {
        AttributeInfo.remove(attribute, ExceptionsAttribute.tag);
    }


    public void setExceptionsAttribute(ExceptionsAttribute cattr) {
        removeExceptionsAttribute();
        if (attribute == null)
            attribute = new ArrayList<AttributeInfo>();

        attribute.add(cattr);
    }


    public void removeCodeAttribute() {
        AttributeInfo.remove(attribute, CodeAttribute.tag);
    }


    public void setCodeAttribute(CodeAttribute cattr) {
        removeCodeAttribute();
        if (attribute == null)
            attribute = new ArrayList<AttributeInfo>();

        attribute.add(cattr);
    }


    public void rebuildStackMapIf6(ClassPool pool, ClassFile cf)
        throws BadBytecode
    {
        if (cf.getMajorVersion() >= ClassFile.JAVA_6)
            rebuildStackMap(pool);

        if (doPreverify)
            rebuildStackMapForME(pool);
    }


    public void rebuildStackMap(ClassPool pool) throws BadBytecode {
        CodeAttribute ca = getCodeAttribute();
        if (ca != null) {
            StackMapTable smt = MapMaker.make(pool, this);
            ca.setAttribute(smt);
        }
    }


    public void rebuildStackMapForME(ClassPool pool) throws BadBytecode {
        CodeAttribute ca = getCodeAttribute();
        if (ca != null) {
            StackMap sm = MapMaker.make2(pool, this);
            ca.setAttribute(sm);
        }
    }


    public int getLineNumber(int pos) {
        CodeAttribute ca = getCodeAttribute();
        if (ca == null)
            return -1;

        LineNumberAttribute ainfo = (LineNumberAttribute)ca
                .getAttribute(LineNumberAttribute.tag);
        if (ainfo == null)
            return -1;

        return ainfo.toLineNumber(pos);
    }


    public void setSuperclass(String superclass) throws BadBytecode {
        if (!isConstructor())
            return;

        CodeAttribute ca = getCodeAttribute();
        byte[] code = ca.getCode();
        CodeIterator iterator = ca.iterator();
        int pos = iterator.skipSuperConstructor();
        if (pos >= 0) {
            ConstPool cp = constPool;
            int mref = ByteArray.readU16bit(code, pos + 1);
            int nt = cp.getMethodrefNameAndType(mref);
            int sc = cp.addClassInfo(superclass);
            int mref2 = cp.addMethodrefInfo(sc, nt);
            ByteArray.write16bit(mref2, code, pos + 1);
        }
    }

    private void read(MethodInfo src, String methodname, Map<String,String> classnames) {
        ConstPool destCp = constPool;
        accessFlags = src.accessFlags;
        name = destCp.addUtf8Info(methodname);
        cachedName = methodname;
        ConstPool srcCp = src.constPool;
        String desc = srcCp.getUtf8Info(src.descriptor);
        String desc2 = Descriptor.rename(desc, classnames);
        descriptor = destCp.addUtf8Info(desc2);

        attribute = new ArrayList<AttributeInfo>();
        ExceptionsAttribute eattr = src.getExceptionsAttribute();
        if (eattr != null)
            attribute.add(eattr.copy(destCp, classnames));

        CodeAttribute cattr = src.getCodeAttribute();
        if (cattr != null)
            attribute.add(cattr.copy(destCp, classnames));
    }

    private void read(DataInputStream in) throws IOException {
        accessFlags = in.readUnsignedShort();
        name = in.readUnsignedShort();
        descriptor = in.readUnsignedShort();
        int n = in.readUnsignedShort();
        attribute = new ArrayList<AttributeInfo>();
        for (int i = 0; i < n; ++i)
            attribute.add(AttributeInfo.read(constPool, in));
    }

    void write(DataOutputStream out) throws IOException {
        out.writeShort(accessFlags);
        out.writeShort(name);
        out.writeShort(descriptor);

        if (attribute == null)
            out.writeShort(0);
        else {
            out.writeShort(attribute.size());
            AttributeInfo.writeAll(attribute, out);
        }
    }
}
