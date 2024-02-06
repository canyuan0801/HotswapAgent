

package org.hotswap.agent.javassist.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.hotswap.agent.javassist.CannotCompileException;


public final class ClassFile {
    int major, minor; 
    ConstPool constPool;
    int thisClass;
    int accessFlags;
    int superClass;
    int[] interfaces;
    List<FieldInfo> fields;
    List<MethodInfo> methods;
    List<AttributeInfo> attributes;
    String thisclassname; 
    String[] cachedInterfaces;
    String cachedSuperclass;

    
    public static final int JAVA_1 = 45;

    
    public static final int JAVA_2 = 46;

    
    public static final int JAVA_3 = 47;

    
    public static final int JAVA_4 = 48;

    
    public static final int JAVA_5 = 49;

    
    public static final int JAVA_6 = 50;

    
    public static final int JAVA_7 = 51;

    
    public static final int JAVA_8 = 52;

    
    public static final int JAVA_9 = 53;

    
    public static final int JAVA_10 = 54;

    
    public static final int JAVA_11 = 55;

    
    public static final int JAVA_12 = 56;

    
    public static final int JAVA_13 = 57;

    
    public static final int JAVA_14 = 58;

    
    public static final int JAVA_15 = 59;

    
    public static final int JAVA_16 = 60;

    
    public static final int JAVA_17 = 61;

    
    public static int MAJOR_VERSION;

    static {
        int ver = JAVA_3;
        try {
            Class.forName("java.lang.StringBuilder");
            ver = JAVA_5;
            Class.forName("java.util.zip.DeflaterInputStream");
            ver = JAVA_6;
            Class.forName("java.lang.invoke.CallSite", false, ClassLoader.getSystemClassLoader());
            ver = JAVA_7;
            Class.forName("java.util.function.Function");
            ver = JAVA_8;
            Class.forName("java.lang.Module");
            ver = JAVA_9;
            List.class.getMethod("copyOf", Collection.class);
            ver = JAVA_10;
            Class.forName("java.util.Optional").getMethod("isEmpty");
            ver = JAVA_11;
            Class.forName("com.sun.source.tree.CaseTree").getMethod("getExpressions");
            ver = JAVA_12;
            Class.forName("com.sun.source.tree.YieldTree");
            ver = JAVA_13;
            Class.forName("com.sun.management.OperatingSystemMXBean").getMethod("getFreeMemorySize");
            ver = JAVA_14;
            Class.forName("java.security.interfaces.EdECPublicKey");
            ver = JAVA_15;
            Class.forName("com.sun.source.tree.PatternTree");
            ver = JAVA_16;
            Class.forName("java.util.random.RandomGenerator");
            ver = JAVA_17;
        } catch (Throwable t) {}
        MAJOR_VERSION = ver;
    }

    
    public ClassFile(DataInputStream in) throws IOException {
        read(in);
    }

    
    public ClassFile(boolean isInterface, String classname, String superclass) {
        major = MAJOR_VERSION;
        minor = 0; 
        constPool = new ConstPool(classname);
        thisClass = constPool.getThisClassInfo();
        if (isInterface)
            accessFlags = AccessFlag.INTERFACE | AccessFlag.ABSTRACT;
        else
            accessFlags = AccessFlag.SUPER;

        initSuperclass(superclass);
        interfaces = null;
        fields = new ArrayList<FieldInfo>();
        methods = new ArrayList<MethodInfo>();
        thisclassname = classname;

        attributes = new ArrayList<AttributeInfo>();
        attributes.add(new SourceFileAttribute(constPool,
                getSourcefileName(thisclassname)));
    }

    private void initSuperclass(String superclass) {
        if (superclass != null) {
            this.superClass = constPool.addClassInfo(superclass);
            cachedSuperclass = superclass;
        }
        else {
            this.superClass = constPool.addClassInfo("java.lang.Object");
            cachedSuperclass = "java.lang.Object";
        }
    }

    private static String getSourcefileName(String qname) {
        return qname.replaceAll("^.*\\.","") + ".java";
    }

    
    public void compact() {
        ConstPool cp = compact0();
        for (MethodInfo minfo:methods)
            minfo.compact(cp);

        for (FieldInfo finfo:fields)
            finfo.compact(cp);

        attributes = AttributeInfo.copyAll(attributes, cp);
        constPool = cp;
    }

    private ConstPool compact0() {
        ConstPool cp = new ConstPool(thisclassname);
        thisClass = cp.getThisClassInfo();
        String sc = getSuperclass();
        if (sc != null)
            superClass = cp.addClassInfo(getSuperclass());

        if (interfaces != null)
            for (int i = 0; i < interfaces.length; ++i)
                interfaces[i]
                    = cp.addClassInfo(constPool.getClassInfo(interfaces[i]));

        return cp;
    }

    
    public void prune() {
        ConstPool cp = compact0();
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

        AttributeInfo signature
            = getAttribute(SignatureAttribute.tag);
        if (signature != null) {
            signature = signature.copy(cp, null);
            newAttributes.add(signature);
        }

        for (MethodInfo minfo:methods)
            minfo.prune(cp);

        for (FieldInfo finfo:fields)
            finfo.prune(cp);

        attributes = newAttributes;
        constPool = cp;
    }

    
    public ConstPool getConstPool() {
        return constPool;
    }

    
    public boolean isInterface() {
        return (accessFlags & AccessFlag.INTERFACE) != 0;
    }

    
    public boolean isFinal() {
        return (accessFlags & AccessFlag.FINAL) != 0;
    }

    
    public boolean isAbstract() {
        return (accessFlags & AccessFlag.ABSTRACT) != 0;
    }

    
    public int getAccessFlags() {
        return accessFlags;
    }

    
    public void setAccessFlags(int acc) {
        if ((acc & AccessFlag.INTERFACE) == 0)
            acc |= AccessFlag.SUPER;

        accessFlags = acc;
    }

    
    public int getInnerAccessFlags() {
        InnerClassesAttribute ica
            = (InnerClassesAttribute)getAttribute(InnerClassesAttribute.tag);
        if (ica == null)
            return -1;

        String name = getName();
        int n = ica.tableLength();
        for (int i = 0; i < n; ++i)
            if (name.equals(ica.innerClass(i)))
                return ica.accessFlags(i);

        return -1;
    }

    
    public String getName() {
        return thisclassname;
    }

    
    public void setName(String name) {
        renameClass(thisclassname, name);
    }

    
    public String getSuperclass() {
        if (cachedSuperclass == null)
            cachedSuperclass = constPool.getClassInfo(superClass);

        return cachedSuperclass;
    }

    
    public int getSuperclassId() {
        return superClass;
    }

    
    public void setSuperclass(String superclass) throws CannotCompileException {
        if (superclass == null)
            superclass = "java.lang.Object";

        try {
            this.superClass = constPool.addClassInfo(superclass);
            for (MethodInfo minfo:methods)
                minfo.setSuperclass(superclass);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
        cachedSuperclass = superclass;
    }

    
    public final void renameClass(String oldname, String newname) {
        if (oldname.equals(newname))
            return;

        if (oldname.equals(thisclassname))
            thisclassname = newname;

        oldname = Descriptor.toJvmName(oldname);
        newname = Descriptor.toJvmName(newname);
        constPool.renameClass(oldname, newname);

        AttributeInfo.renameClass(attributes, oldname, newname);
        for (MethodInfo minfo :methods) {
            String desc = minfo.getDescriptor();
            minfo.setDescriptor(Descriptor.rename(desc, oldname, newname));
            AttributeInfo.renameClass(minfo.getAttributes(), oldname, newname);
        }

        for (FieldInfo finfo:fields) {
            String desc = finfo.getDescriptor();
            finfo.setDescriptor(Descriptor.rename(desc, oldname, newname));
            AttributeInfo.renameClass(finfo.getAttributes(), oldname, newname);
        }
    }

    
    public final void renameClass(Map<String,String> classnames) {
        String jvmNewThisName = classnames.get(Descriptor
                .toJvmName(thisclassname));
        if (jvmNewThisName != null)
            thisclassname = Descriptor.toJavaName(jvmNewThisName);

        constPool.renameClass(classnames);

        AttributeInfo.renameClass(attributes, classnames);
        for (MethodInfo minfo:methods) {
            String desc = minfo.getDescriptor();
            minfo.setDescriptor(Descriptor.rename(desc, classnames));
            AttributeInfo.renameClass(minfo.getAttributes(), classnames);
        }

        for (FieldInfo finfo:fields) {
            String desc = finfo.getDescriptor();
            finfo.setDescriptor(Descriptor.rename(desc, classnames));
            AttributeInfo.renameClass(finfo.getAttributes(), classnames);
        }
    }

    
    public final void getRefClasses(Map<String,String> classnames) {
        constPool.renameClass(classnames);

        AttributeInfo.getRefClasses(attributes, classnames);
        for (MethodInfo minfo:methods) {
            String desc = minfo.getDescriptor();
            Descriptor.rename(desc, classnames);
            AttributeInfo.getRefClasses(minfo.getAttributes(), classnames);
        }

        for (FieldInfo finfo:fields) {
            String desc = finfo.getDescriptor();
            Descriptor.rename(desc, classnames);
            AttributeInfo.getRefClasses(finfo.getAttributes(), classnames);
        }
    }

    
    public String[] getInterfaces() {
        if (cachedInterfaces != null)
            return cachedInterfaces;

        String[] rtn = null;
        if (interfaces == null)
            rtn = new String[0];
        else {
            String[] list = new String[interfaces.length];
            for (int i = 0; i < interfaces.length; ++i)
                list[i] = constPool.getClassInfo(interfaces[i]);

            rtn = list;
        }

        cachedInterfaces = rtn;
        return rtn;
    }

    
    public void setInterfaces(String[] nameList) {
        cachedInterfaces = null;
        if (nameList != null) {
            interfaces = new int[nameList.length];
            for (int i = 0; i < nameList.length; ++i)
                interfaces[i] = constPool.addClassInfo(nameList[i]);
        }
    }

    
    public void addInterface(String name) {
        cachedInterfaces = null;
        int info = constPool.addClassInfo(name);
        if (interfaces == null) {
            interfaces = new int[1];
            interfaces[0] = info;
        }
        else {
            int n = interfaces.length;
            int[] newarray = new int[n + 1];
            System.arraycopy(interfaces, 0, newarray, 0, n);
            newarray[n] = info;
            interfaces = newarray;
        }
    }

    
    public List<FieldInfo> getFields() {
        return fields;
    }

    
    public void addField(FieldInfo finfo) throws DuplicateMemberException {
        testExistingField(finfo.getName(), finfo.getDescriptor());
        fields.add(finfo);
    }

    
    public final void addField2(FieldInfo finfo) {
        fields.add(finfo);
    }

    private void testExistingField(String name, String descriptor)
            throws DuplicateMemberException {
        for (FieldInfo minfo:fields)
            if (minfo.getName().equals(name))
                throw new DuplicateMemberException("duplicate field: " + name);
    }

    
    public List<MethodInfo> getMethods() {
        return methods;
    }

    
    public MethodInfo getMethod(String name) {
        for (MethodInfo minfo:methods)
            if (minfo.getName().equals(name))
                return minfo;
        return null;
    }

    
    public MethodInfo getStaticInitializer() {
        return getMethod(MethodInfo.nameClinit);
    }

    
    public void addMethod(MethodInfo minfo) throws DuplicateMemberException {
        testExistingMethod(minfo);
        methods.add(minfo);
    }

    
    public final void addMethod2(MethodInfo minfo) {
        methods.add(minfo);
    }

    private void testExistingMethod(MethodInfo newMinfo)
        throws DuplicateMemberException
    {
        String name = newMinfo.getName();
        String descriptor = newMinfo.getDescriptor();
        ListIterator<MethodInfo> it = methods.listIterator(0);
        while (it.hasNext())
            if (isDuplicated(newMinfo, name, descriptor, it.next(), it))
                throw new DuplicateMemberException("duplicate method: " + name
                                                   + " in " + this.getName());
    }

    private static boolean isDuplicated(MethodInfo newMethod, String newName,
                                        String newDesc, MethodInfo minfo,
                                        ListIterator<MethodInfo> it)
    {
        if (!minfo.getName().equals(newName))
            return false;

        String desc = minfo.getDescriptor();
        if (!Descriptor.eqParamTypes(desc, newDesc))
           return false;

        if (desc.equals(newDesc)) {
            if (notBridgeMethod(minfo))
                return true;
                
                
            it.remove();
            return false;
        }
            return false;
           
    }

    
    private static boolean notBridgeMethod(MethodInfo minfo) {
        return (minfo.getAccessFlags() & AccessFlag.BRIDGE) == 0;
    }

    
    public List<AttributeInfo> getAttributes() {
        return attributes;
    }

    
    public AttributeInfo getAttribute(String name) {
        for (AttributeInfo ai:attributes)
            if (ai.getName().equals(name))
                return ai;
        return null;
    }

    
    public AttributeInfo removeAttribute(String name) {
        return AttributeInfo.remove(attributes, name);
    }

    
    public void addAttribute(AttributeInfo info) {
        AttributeInfo.remove(attributes, info.getName());
        attributes.add(info);
    }

    
    public String getSourceFile() {
        SourceFileAttribute sf
            = (SourceFileAttribute)getAttribute(SourceFileAttribute.tag);
        if (sf == null)
            return null;
        return sf.getFileName();
    }

    private void read(DataInputStream in) throws IOException {
        int i, n;
        int magic = in.readInt();
        if (magic != 0xCAFEBABE)
            throw new IOException("bad magic number: " + Integer.toHexString(magic));

        minor = in.readUnsignedShort();
        major = in.readUnsignedShort();
        constPool = new ConstPool(in);
        accessFlags = in.readUnsignedShort();
        thisClass = in.readUnsignedShort();
        constPool.setThisClassInfo(thisClass);
        superClass = in.readUnsignedShort();
        n = in.readUnsignedShort();
        if (n == 0)
            interfaces = null;
        else {
            interfaces = new int[n];
            for (i = 0; i < n; ++i)
                interfaces[i] = in.readUnsignedShort();
        }

        ConstPool cp = constPool;
        n = in.readUnsignedShort();
        fields = new ArrayList<FieldInfo>();
        for (i = 0; i < n; ++i)
            addField2(new FieldInfo(cp, in));

        n = in.readUnsignedShort();
        methods = new ArrayList<MethodInfo>();
        for (i = 0; i < n; ++i)
            addMethod2(new MethodInfo(cp, in));

        attributes = new ArrayList<AttributeInfo>();
        n = in.readUnsignedShort();
        for (i = 0; i < n; ++i)
            addAttribute(AttributeInfo.read(cp, in));

        thisclassname = constPool.getClassInfo(thisClass);
    }

    
    public void write(DataOutputStream out) throws IOException {
        int i, n;

        out.writeInt(0xCAFEBABE); 
        out.writeShort(minor); 
        out.writeShort(major); 
        constPool.write(out); 
        out.writeShort(accessFlags);
        out.writeShort(thisClass);
        out.writeShort(superClass);

        if (interfaces == null)
            n = 0;
        else
            n = interfaces.length;

        out.writeShort(n);
        for (i = 0; i < n; ++i)
            out.writeShort(interfaces[i]);

        n = fields.size();
        out.writeShort(n);
        for (i = 0; i < n; ++i) {
            FieldInfo finfo = fields.get(i);
            finfo.write(out);
        }

        out.writeShort(methods.size());
        for (MethodInfo minfo:methods)
            minfo.write(out);

        out.writeShort(attributes.size());
        AttributeInfo.writeAll(attributes, out);
    }

    
    public int getMajorVersion() {
        return major;
    }

    
    public void setMajorVersion(int major) {
        this.major = major;
    }

    
    public int getMinorVersion() {
        return minor;
    }

    
    public void setMinorVersion(int minor) {
        this.minor = minor;
    }

    
    public void setVersionToJava5() {
        this.major = 49;
        this.minor = 0;
    }
}
