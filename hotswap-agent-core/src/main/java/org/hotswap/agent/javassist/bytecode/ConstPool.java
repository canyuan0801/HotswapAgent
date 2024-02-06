

package org.hotswap.agent.javassist.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hotswap.agent.javassist.CtClass;


public final class ConstPool
{
    LongVector items;
    int numOfItems;
    int thisClassInfo;
    Map<ConstInfo,ConstInfo> itemsCache;


    public static final int CONST_Class = ClassInfo.tag;


    public static final int CONST_Fieldref = FieldrefInfo.tag;


    public static final int CONST_Methodref = MethodrefInfo.tag;


    public static final int CONST_InterfaceMethodref
                                        = InterfaceMethodrefInfo.tag;


    public static final int CONST_String = StringInfo.tag;


    public static final int CONST_Integer = IntegerInfo.tag;


    public static final int CONST_Float = FloatInfo.tag;


    public static final int CONST_Long = LongInfo.tag;


    public static final int CONST_Double = DoubleInfo.tag;


    public static final int CONST_NameAndType = NameAndTypeInfo.tag;


    public static final int CONST_Utf8 = Utf8Info.tag;


    public static final int CONST_MethodHandle = MethodHandleInfo.tag;


    public static final int CONST_MethodType = MethodTypeInfo.tag;


    public static final int CONST_InvokeDynamic = InvokeDynamicInfo.tag;


    public static final int CONST_Module = ModuleInfo.tag;


    public static final int CONST_Package = PackageInfo.tag;


    public static final CtClass THIS = null;


    public static final int REF_getField = 1;


    public static final int REF_getStatic = 2;


    public static final int REF_putField = 3;


    public static final int REF_putStatic = 4;


    public static final int REF_invokeVirtual = 5;


    public static final int REF_invokeStatic = 6;


    public static final int REF_invokeSpecial = 7;


    public static final int REF_newInvokeSpecial = 8;


    public static final int REF_invokeInterface = 9;


    public ConstPool(String thisclass)
    {
        items = new LongVector();
        itemsCache = null;
        numOfItems = 0;
        addItem0(null);
        thisClassInfo = addClassInfo(thisclass);
    }


    public ConstPool(DataInputStream in) throws IOException
    {
        itemsCache = null;
        thisClassInfo = 0;

        read(in);
    }

    void prune()
    {
        itemsCache = null;
    }


    public int getSize()
    {
        return numOfItems;
    }


    public String getClassName()
    {
        return getClassInfo(thisClassInfo);
    }


    public int getThisClassInfo()
    {
        return thisClassInfo;
    }

    void setThisClassInfo(int i)
    {
        thisClassInfo = i;
    }

    ConstInfo getItem(int n)
    {
        return items.elementAt(n);
    }


    public int getTag(int index)
    {
        return getItem(index).getTag();
    }


    public String getClassInfo(int index)
    {
        ClassInfo c = (ClassInfo)getItem(index);
        if (c == null)
            return null;
        return Descriptor.toJavaName(getUtf8Info(c.name));
    }


    public String getClassInfoByDescriptor(int index)
    {
        ClassInfo c = (ClassInfo)getItem(index);
        if (c == null)
            return null;
        String className = getUtf8Info(c.name);
        if (className.charAt(0) == '[')
            return className;
        return Descriptor.of(className);
    }


    public int getNameAndTypeName(int index)
    {
        NameAndTypeInfo ntinfo = (NameAndTypeInfo)getItem(index);
        return ntinfo.memberName;
    }


    public int getNameAndTypeDescriptor(int index)
    {
        NameAndTypeInfo ntinfo = (NameAndTypeInfo)getItem(index);
        return ntinfo.typeDescriptor;
    }


    public int getMemberClass(int index)
    {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        return minfo.classIndex;
    }


    public int getMemberNameAndType(int index)
    {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        return minfo.nameAndTypeIndex;
    }


    public int getFieldrefClass(int index)
    {
        FieldrefInfo finfo = (FieldrefInfo)getItem(index);
        return finfo.classIndex;
    }


    public String getFieldrefClassName(int index)
    {
        FieldrefInfo f = (FieldrefInfo)getItem(index);
        if (f == null)
            return null;
        return getClassInfo(f.classIndex);
    }


    public int getFieldrefNameAndType(int index)
    {
        FieldrefInfo finfo = (FieldrefInfo)getItem(index);
        return finfo.nameAndTypeIndex;
    }


    public String getFieldrefName(int index)
    {
        FieldrefInfo f = (FieldrefInfo)getItem(index);
        if (f == null)
            return null;
        NameAndTypeInfo n = (NameAndTypeInfo)getItem(f.nameAndTypeIndex);
        if(n == null)
            return null;
        return getUtf8Info(n.memberName);
    }


    public String getFieldrefType(int index)
    {
        FieldrefInfo f = (FieldrefInfo)getItem(index);
        if (f == null)
            return null;
        NameAndTypeInfo n = (NameAndTypeInfo)getItem(f.nameAndTypeIndex);
        if(n == null)
            return null;
        return getUtf8Info(n.typeDescriptor);
    }


    public int getMethodrefClass(int index)
    {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        return minfo.classIndex;
    }


    public String getMethodrefClassName(int index)
    {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        if (minfo == null)
            return null;
        return getClassInfo(minfo.classIndex);
    }


    public int getMethodrefNameAndType(int index)
    {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        return minfo.nameAndTypeIndex;
    }


    public String getMethodrefName(int index)
    {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        if (minfo == null)
            return null;
        NameAndTypeInfo n
            = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
        if(n == null)
            return null;
        return getUtf8Info(n.memberName);
    }


    public String getMethodrefType(int index)
    {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        if (minfo == null)
            return null;
        NameAndTypeInfo n
            = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
        if(n == null)
            return null;
        return getUtf8Info(n.typeDescriptor);
    }


    public int getInterfaceMethodrefClass(int index)
    {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        return minfo.classIndex;
    }


    public String getInterfaceMethodrefClassName(int index)
    {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        return getClassInfo(minfo.classIndex);
    }


    public int getInterfaceMethodrefNameAndType(int index)
    {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        return minfo.nameAndTypeIndex;
    }


    public String getInterfaceMethodrefName(int index)
    {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        if (minfo == null)
            return null;
        NameAndTypeInfo n
            = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
        if(n == null)
            return null;
        return getUtf8Info(n.memberName);
    }


    public String getInterfaceMethodrefType(int index)
    {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        if (minfo == null)
            return null;
        NameAndTypeInfo n
            = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
        if(n == null)
            return null;
        return getUtf8Info(n.typeDescriptor);
    }

    public Object getLdcValue(int index)
    {
        ConstInfo constInfo = this.getItem(index);
        Object value = null;
        if (constInfo instanceof StringInfo)
            value = this.getStringInfo(index);
        else if (constInfo instanceof FloatInfo)
            value = Float.valueOf(getFloatInfo(index));
        else if (constInfo instanceof IntegerInfo)
            value = Integer.valueOf(getIntegerInfo(index));
        else if (constInfo instanceof LongInfo)
            value = Long.valueOf(getLongInfo(index));
        else if (constInfo instanceof DoubleInfo)
            value = Double.valueOf(getDoubleInfo(index));

        return value;
    }


    public int getIntegerInfo(int index)
    {
        IntegerInfo i = (IntegerInfo)getItem(index);
        return i.value;
    }


    public float getFloatInfo(int index)
    {
        FloatInfo i = (FloatInfo)getItem(index);
        return i.value;
    }


    public long getLongInfo(int index)
    {
        LongInfo i = (LongInfo)getItem(index);
        return i.value;
    }


    public double getDoubleInfo(int index)
    {
        DoubleInfo i = (DoubleInfo)getItem(index);
        return i.value;
    }


    public String getStringInfo(int index)
    {
        StringInfo si = (StringInfo)getItem(index);
        return getUtf8Info(si.string);
    }


    public String getUtf8Info(int index)
    {
        Utf8Info utf = (Utf8Info)getItem(index);
        return utf.string;
    }


    public int getMethodHandleKind(int index)
    {
        MethodHandleInfo mhinfo = (MethodHandleInfo)getItem(index);
        return mhinfo.refKind;
    }


    public int getMethodHandleIndex(int index)
    {
        MethodHandleInfo mhinfo = (MethodHandleInfo)getItem(index);
        return mhinfo.refIndex;
    }


    public int getMethodTypeInfo(int index)
    {
        MethodTypeInfo mtinfo = (MethodTypeInfo)getItem(index);
        return mtinfo.descriptor;
    }


    public int getInvokeDynamicBootstrap(int index)
    {
        InvokeDynamicInfo iv = (InvokeDynamicInfo)getItem(index);
        return iv.bootstrap;
    }


    public int getInvokeDynamicNameAndType(int index)
    {
        InvokeDynamicInfo iv = (InvokeDynamicInfo)getItem(index);
        return iv.nameAndType;
    }


    public String getInvokeDynamicType(int index)
    {
        InvokeDynamicInfo iv = (InvokeDynamicInfo)getItem(index);
        if (iv == null)
            return null;
        NameAndTypeInfo n = (NameAndTypeInfo)getItem(iv.nameAndType);
        if(n == null)
            return null;
        return getUtf8Info(n.typeDescriptor);
    }


    public String getModuleInfo(int index)
    {
        ModuleInfo mi = (ModuleInfo)getItem(index);
        return getUtf8Info(mi.name);
    }


    public String getPackageInfo(int index)
    {
        PackageInfo mi = (PackageInfo)getItem(index);
        return getUtf8Info(mi.name);
    }


    public int isConstructor(String classname, int index)
    {
        return isMember(classname, MethodInfo.nameInit, index);
    }


    public int isMember(String classname, String membername, int index)
    {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        if (getClassInfo(minfo.classIndex).equals(classname)) {
            NameAndTypeInfo ntinfo
                = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
            if (getUtf8Info(ntinfo.memberName).equals(membername))
                return ntinfo.typeDescriptor;
        }

        return 0;
    }


    public String eqMember(String membername, String desc, int index)
    {
        MemberrefInfo minfo = (MemberrefInfo)getItem(index);
        NameAndTypeInfo ntinfo
                = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
        if (getUtf8Info(ntinfo.memberName).equals(membername)
            && getUtf8Info(ntinfo.typeDescriptor).equals(desc))
            return getClassInfo(minfo.classIndex);
        return null;
    }

    private int addItem0(ConstInfo info)
    {
        items.addElement(info);
        return numOfItems++;
    }

    private int addItem(ConstInfo info)
    {
        if (itemsCache == null)
            itemsCache = makeItemsCache(items);

        ConstInfo found = itemsCache.get(info);
        if (found != null)
            return found.index;
        items.addElement(info);
        itemsCache.put(info, info);
        return numOfItems++;
    }


    public int copy(int n, ConstPool dest, Map<String,String> classnames)
    {
        if (n == 0)
            return 0;

        ConstInfo info = getItem(n);
        return info.copy(this, dest, classnames);
    }

    int addConstInfoPadding() {
        return addItem0(new ConstInfoPadding(numOfItems));
    }


    public int addClassInfo(CtClass c)
    {
        if (c == THIS)
            return thisClassInfo;
        else if (!c.isArray())
            return addClassInfo(c.getName());
        else {





            return addClassInfo(Descriptor.toJvmName(c));
        }
    }


    public int addClassInfo(String qname)
    {
        int utf8 = addUtf8Info(Descriptor.toJvmName(qname));
        return addItem(new ClassInfo(utf8, numOfItems));
    }


    public int addNameAndTypeInfo(String name, String type)
    {
        return addNameAndTypeInfo(addUtf8Info(name), addUtf8Info(type));
    }


    public int addNameAndTypeInfo(int name, int type)
    {
        return addItem(new NameAndTypeInfo(name, type, numOfItems));
    }


    public int addFieldrefInfo(int classInfo, String name, String type)
    {
        int nt = addNameAndTypeInfo(name, type);
        return addFieldrefInfo(classInfo, nt);
    }


    public int addFieldrefInfo(int classInfo, int nameAndTypeInfo) 
    {
        return addItem(new FieldrefInfo(classInfo, nameAndTypeInfo,
                                        numOfItems));
    }


    public int addMethodrefInfo(int classInfo, String name, String type)
    {
        int nt = addNameAndTypeInfo(name, type);
        return addMethodrefInfo(classInfo, nt);
    }


    public int addMethodrefInfo(int classInfo, int nameAndTypeInfo)
    {
         return addItem(new MethodrefInfo(classInfo,
                 nameAndTypeInfo, numOfItems));
    }


    public int addInterfaceMethodrefInfo(int classInfo,
                                         String name,
                                         String type)
    {
        int nt = addNameAndTypeInfo(name, type);
        return addInterfaceMethodrefInfo(classInfo, nt);
    }


    public int addInterfaceMethodrefInfo(int classInfo,
                                         int nameAndTypeInfo)
    {
        return addItem(new InterfaceMethodrefInfo(classInfo,
                                                  nameAndTypeInfo,
                                                  numOfItems));
    }


    public int addStringInfo(String str)
    {
        int utf = addUtf8Info(str);
        return addItem(new StringInfo(utf, numOfItems));
    }


    public int addIntegerInfo(int i)
    {
        return addItem(new IntegerInfo(i, numOfItems));
    }


    public int addFloatInfo(float f)
    {
        return addItem(new FloatInfo(f, numOfItems));
    }


    public int addLongInfo(long l)
    {
        int i = addItem(new LongInfo(l, numOfItems));
        if (i == numOfItems - 1)
            addConstInfoPadding();

        return i;
    }


    public int addDoubleInfo(double d)
    {
        int i = addItem(new DoubleInfo(d, numOfItems));
        if (i == numOfItems - 1)
            addConstInfoPadding();

        return i;
    }


    public int addUtf8Info(String utf8)
    {
        return addItem(new Utf8Info(utf8, numOfItems));
    }


    public int addMethodHandleInfo(int kind, int index)
    {
        return addItem(new MethodHandleInfo(kind, index, numOfItems));
    }


    public int addMethodTypeInfo(int desc)
    {
        return addItem(new MethodTypeInfo(desc, numOfItems));
    }


    public int addInvokeDynamicInfo(int bootstrap, int nameAndType)
    {
        return addItem(new InvokeDynamicInfo(bootstrap, nameAndType, numOfItems));
    }


    public int addModuleInfo(int nameIndex)
    {
        return addItem(new ModuleInfo(nameIndex, numOfItems));
    }


    public int addPackageInfo(int nameIndex)
    {
        return addItem(new PackageInfo(nameIndex, numOfItems));
    }


    public Set<String> getClassNames()
    {
        Set<String> result = new HashSet<String>();
        LongVector v = items;
        int size = numOfItems;
        for (int i = 1; i < size; ++i) {
            String className = v.elementAt(i).getClassName(this);
            if (className != null)
               result.add(className);
        }
        return result;
    }


    public void renameClass(String oldName, String newName)
    {
        LongVector v = items;
        int size = numOfItems;
        for (int i = 1; i < size; ++i) {
            ConstInfo ci = v.elementAt(i);
            ci.renameClass(this, oldName, newName, itemsCache);
        }
    }


    public void renameClass(Map<String,String> classnames)
    {
        LongVector v = items;
        int size = numOfItems;
        for (int i = 1; i < size; ++i) {
            ConstInfo ci = v.elementAt(i);
            ci.renameClass(this, classnames, itemsCache);
        }
    }

    private void read(DataInputStream in) throws IOException
    {
        int n = in.readUnsignedShort();

        items = new LongVector(n);
        numOfItems = 0;
        addItem0(null);

        while (--n > 0) {
            int tag = readOne(in);
            if ((tag == LongInfo.tag) || (tag == DoubleInfo.tag)) {
                addConstInfoPadding();
                --n;
            }
        }
    }

    private static Map<ConstInfo,ConstInfo> makeItemsCache(LongVector items)
    {
        Map<ConstInfo,ConstInfo> cache = new HashMap<ConstInfo,ConstInfo>();
        int i = 1;
        while (true) {
            ConstInfo info = items.elementAt(i++);
            if (info == null)
                break;
            cache.put(info, info);
        }

        return cache;
    }

    private int readOne(DataInputStream in) throws IOException
    {
        ConstInfo info;
        int tag = in.readUnsignedByte();
        switch (tag) {
        case Utf8Info.tag :
            info = new Utf8Info(in, numOfItems);
            break;
        case IntegerInfo.tag :
            info = new IntegerInfo(in, numOfItems);
            break;
        case FloatInfo.tag :
            info = new FloatInfo(in, numOfItems);
            break;
        case LongInfo.tag :
            info = new LongInfo(in, numOfItems);
            break;
        case DoubleInfo.tag :
            info = new DoubleInfo(in, numOfItems);
            break;
        case ClassInfo.tag :
            info = new ClassInfo(in, numOfItems);
            break;
        case StringInfo.tag :
            info = new StringInfo(in, numOfItems);
            break;
        case FieldrefInfo.tag :
            info = new FieldrefInfo(in, numOfItems);
            break;
        case MethodrefInfo.tag :
            info = new MethodrefInfo(in, numOfItems);
            break;
        case InterfaceMethodrefInfo.tag :
            info = new InterfaceMethodrefInfo(in, numOfItems);
            break;
        case NameAndTypeInfo.tag :
            info = new NameAndTypeInfo(in, numOfItems);
            break;
        case MethodHandleInfo.tag :
            info = new MethodHandleInfo(in, numOfItems);
            break;
        case MethodTypeInfo.tag :
            info = new MethodTypeInfo(in, numOfItems);
            break;
        case InvokeDynamicInfo.tag :
            info = new InvokeDynamicInfo(in, numOfItems);
            break;
        case ModuleInfo.tag :
            info = new ModuleInfo(in, numOfItems);
            break;
        case PackageInfo.tag :
            info = new PackageInfo(in, numOfItems);
            break;
        default :
            throw new IOException("invalid constant type: " 
                                + tag + " at " + numOfItems);
        }

        addItem0(info);
        return tag;
    }


    public void write(DataOutputStream out) throws IOException
    {
        out.writeShort(numOfItems);
        LongVector v = items;
        int size = numOfItems;
        for (int i = 1; i < size; ++i)
            v.elementAt(i).write(out);
    }


    public void print()
    {
        print(new PrintWriter(System.out, true));
    }


    public void print(PrintWriter out)
    {
        int size = numOfItems;
        for (int i = 1; i < size; ++i) {
            out.print(i);
            out.print(" ");
            items.elementAt(i).print(out);
        }
    }
}

abstract class ConstInfo
{
    int index;

    public ConstInfo(int i) { index = i; }

    public abstract int getTag();

    public String getClassName(ConstPool cp) { return null; }
    public void renameClass(ConstPool cp, String oldName, String newName,
            Map<ConstInfo,ConstInfo> cache) {}
    public void renameClass(ConstPool cp, Map<String,String> classnames,
            Map<ConstInfo,ConstInfo> cache) {}
    public abstract int copy(ConstPool src, ConstPool dest,
            Map<String, String> classnames);


    public abstract void write(DataOutputStream out) throws IOException;
    public abstract void print(PrintWriter out);

    @Override
    public String toString() {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(bout);
        print(out);
        return bout.toString();
    }
}


class ConstInfoPadding extends ConstInfo
{
    public ConstInfoPadding(int i) { super(i); }

    @Override
    public int getTag() { return 0; }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<String,String> map)
    {
        return dest.addConstInfoPadding();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {}

    @Override
    public void print(PrintWriter out)
    {
        out.println("padding");
    }
}

class ClassInfo extends ConstInfo
{
    static final int tag = 7;
    int name;

    public ClassInfo(int className, int index)
    {
        super(index);
        name = className;
    }

    public ClassInfo(DataInputStream in, int index) throws IOException
    {
        super(index);
        name = in.readUnsignedShort();
    }

    @Override
    public int hashCode() { return name; }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof ClassInfo && ((ClassInfo)obj).name == name;
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public String getClassName(ConstPool cp)
    {
        return cp.getUtf8Info(name);
    }

    @Override
    public void renameClass(ConstPool cp, String oldName, String newName,
            Map<ConstInfo,ConstInfo> cache)
    {
        String nameStr = cp.getUtf8Info(name);
        String newNameStr = null;
        if (nameStr.equals(oldName))
            newNameStr = newName;
        else if (nameStr.charAt(0) == '[') {
            String s = Descriptor.rename(nameStr, oldName, newName);
            if (nameStr != s)
                newNameStr = s;
        }

        if (newNameStr != null)
            if (cache == null)
                name = cp.addUtf8Info(newNameStr);
            else {
                cache.remove(this);
                name = cp.addUtf8Info(newNameStr);
                cache.put(this, this);
            }
    }

    @Override
    public void renameClass(ConstPool cp, Map<String,String> map,
            Map<ConstInfo,ConstInfo> cache)
    {
        String oldName = cp.getUtf8Info(name);
        String newName = null;
        if (oldName.charAt(0) == '[') {
            String s = Descriptor.rename(oldName, map);
            if (oldName != s)
                newName = s;
        }
        else {
            String s = map.get(oldName);
            if (s != null && !s.equals(oldName))
                newName = s;
        }

        if (newName != null) {
            if (cache == null)
                name = cp.addUtf8Info(newName);
            else {
                cache.remove(this);
                name = cp.addUtf8Info(newName);
                cache.put(this, this);
            }
        }
    }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<String,String> map)
    {
        String classname = src.getUtf8Info(name);
        if (map != null) {
            String newname = map.get(classname);
            if (newname != null)
                classname = newname;
        }

        return dest.addClassInfo(classname);
    }

    @Override
    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(tag);
        out.writeShort(name);
    }

    @Override
    public void print(PrintWriter out)
    {
        out.print("Class #");
        out.println(name);
    }
}

class NameAndTypeInfo extends ConstInfo
{
    static final int tag = 12;
    int memberName;
    int typeDescriptor;

    public NameAndTypeInfo(int name, int type, int index)
    {
        super(index);
        memberName = name;
        typeDescriptor = type;
    }

    public NameAndTypeInfo(DataInputStream in, int index) throws IOException
    {
        super(index);
        memberName = in.readUnsignedShort();
        typeDescriptor = in.readUnsignedShort();
    }

    @Override
    public int hashCode() { return (memberName << 16) ^ typeDescriptor; }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof NameAndTypeInfo) {
            NameAndTypeInfo nti = (NameAndTypeInfo)obj;
            return nti.memberName == memberName
                    && nti.typeDescriptor == typeDescriptor;
        }
        return false;
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public void renameClass(ConstPool cp, String oldName, String newName,
            Map<ConstInfo,ConstInfo> cache)
    {
        String type = cp.getUtf8Info(typeDescriptor);
        String type2 = Descriptor.rename(type, oldName, newName);
        if (type != type2)
            if (cache == null)
                typeDescriptor = cp.addUtf8Info(type2);
            else {
                cache.remove(this);
                typeDescriptor = cp.addUtf8Info(type2);
                cache.put(this, this);
            }
    }

    @Override
    public void renameClass(ConstPool cp, Map<String,String> map,
            Map<ConstInfo,ConstInfo> cache)
    {
        String type = cp.getUtf8Info(typeDescriptor);
        String type2 = Descriptor.rename(type, map);
        if (type != type2)
            if (cache == null)
                typeDescriptor = cp.addUtf8Info(type2);
            else {
                cache.remove(this);
                typeDescriptor = cp.addUtf8Info(type2);
                cache.put(this, this);
            }
    }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<String,String> map)
    {
        String mname = src.getUtf8Info(memberName);
        String tdesc = src.getUtf8Info(typeDescriptor);
        tdesc = Descriptor.rename(tdesc, map);
        return dest.addNameAndTypeInfo(dest.addUtf8Info(mname),
                                       dest.addUtf8Info(tdesc));
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeByte(tag);
        out.writeShort(memberName);
        out.writeShort(typeDescriptor);
    }

    @Override
    public void print(PrintWriter out) {
        out.print("NameAndType #");
        out.print(memberName);
        out.print(", type #");
        out.println(typeDescriptor);
    }
}

abstract class MemberrefInfo extends ConstInfo
{
    int classIndex;
    int nameAndTypeIndex;

    public MemberrefInfo(int cindex, int ntindex, int thisIndex)
    {
        super(thisIndex);
        classIndex = cindex;
        nameAndTypeIndex = ntindex;
    }

    public MemberrefInfo(DataInputStream in, int thisIndex)
            throws IOException
    {
        super(thisIndex);
        classIndex = in.readUnsignedShort();
        nameAndTypeIndex = in.readUnsignedShort();
    }

    @Override
    public int hashCode() { return (classIndex << 16) ^ nameAndTypeIndex; }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MemberrefInfo) {
            MemberrefInfo mri = (MemberrefInfo)obj;
            return mri.classIndex == classIndex
                    && mri.nameAndTypeIndex == nameAndTypeIndex
                    && mri.getClass() == this.getClass();
        }
        return false;
    }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<String,String> map)
    {
        int classIndex2 = src.getItem(classIndex).copy(src, dest, map);
        int ntIndex2 = src.getItem(nameAndTypeIndex).copy(src, dest, map);
        return copy2(dest, classIndex2, ntIndex2);
    }

    abstract protected int copy2(ConstPool dest, int cindex, int ntindex);

    @Override
    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(getTag());
        out.writeShort(classIndex);
        out.writeShort(nameAndTypeIndex);
    }

    @Override
    public void print(PrintWriter out)
    {
        out.print(getTagName() + " #");
        out.print(classIndex);
        out.print(", name&type #");
        out.println(nameAndTypeIndex);
    }

    public abstract String getTagName();
}

class FieldrefInfo extends MemberrefInfo
{
    static final int tag = 9;

    public FieldrefInfo(int cindex, int ntindex, int thisIndex)
    {
        super(cindex, ntindex, thisIndex);
    }

    public FieldrefInfo(DataInputStream in, int thisIndex)
            throws IOException
    {
        super(in, thisIndex);
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public String getTagName() { return "Field"; }

    @Override
    protected int copy2(ConstPool dest, int cindex, int ntindex)
    {
        return dest.addFieldrefInfo(cindex, ntindex);
    }
}

class MethodrefInfo extends MemberrefInfo
{
    static final int tag = 10;

    public MethodrefInfo(int cindex, int ntindex, int thisIndex)
    {
        super(cindex, ntindex, thisIndex);
    }

    public MethodrefInfo(DataInputStream in, int thisIndex)
            throws IOException
    {
        super(in, thisIndex);
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public String getTagName() { return "Method"; }

    @Override
    protected int copy2(ConstPool dest, int cindex, int ntindex)
    {
        return dest.addMethodrefInfo(cindex, ntindex);
    }
}

class InterfaceMethodrefInfo extends MemberrefInfo
{
    static final int tag = 11;

    public InterfaceMethodrefInfo(int cindex, int ntindex, int thisIndex)
    {
        super(cindex, ntindex, thisIndex);
    }

    public InterfaceMethodrefInfo(DataInputStream in, int thisIndex)
            throws IOException
    {
        super(in, thisIndex);
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public String getTagName() { return "Interface"; }

    @Override
    protected int copy2(ConstPool dest, int cindex, int ntindex)
    {
        return dest.addInterfaceMethodrefInfo(cindex, ntindex);
    }
}

class StringInfo extends ConstInfo
{
    static final int tag = 8;
    int string;

    public StringInfo(int str, int index)
    {
        super(index);
        string = str;
    }

    public StringInfo(DataInputStream in, int index) throws IOException
    {
        super(index);
        string = in.readUnsignedShort();
    }

    @Override
    public int hashCode() { return string; }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof StringInfo && ((StringInfo)obj).string == string;
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<String,String> map)
    {
        return dest.addStringInfo(src.getUtf8Info(string));
    }

    @Override
    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(tag);
        out.writeShort(string);
    }

    @Override
    public void print(PrintWriter out)
    {
        out.print("String #");
        out.println(string);
    }
}

class IntegerInfo extends ConstInfo
{
    static final int tag = 3;
    int value;

    public IntegerInfo(int v, int index)
    {
        super(index);
        value = v;
    }

    public IntegerInfo(DataInputStream in, int index) throws IOException
    {
        super(index);
        value = in.readInt();
    }

    @Override
    public int hashCode() { return value; }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof IntegerInfo && ((IntegerInfo)obj).value == value;
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<String,String> map)
    {
        return dest.addIntegerInfo(value);
    }

    @Override
    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(tag);
        out.writeInt(value);
    }

    @Override
    public void print(PrintWriter out)
    {
        out.print("Integer ");
        out.println(value);
    }
}

class FloatInfo extends ConstInfo
{
    static final int tag = 4;
    float value;

    public FloatInfo(float f, int index)
    {
        super(index);
        value = f;
    }

    public FloatInfo(DataInputStream in, int index) throws IOException
    {
        super(index);
        value = in.readFloat();
    }

    @Override
    public int hashCode() { return Float.floatToIntBits(value); }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof FloatInfo && ((FloatInfo)obj).value == value;
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<String,String> map)
    {
        return dest.addFloatInfo(value);
    }

    @Override
    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(tag);
        out.writeFloat(value);
    }

    @Override
    public void print(PrintWriter out)
    {
        out.print("Float ");
        out.println(value);
    }
}

class LongInfo extends ConstInfo
{
    static final int tag = 5;
    long value;

    public LongInfo(long l, int index)
    {
        super(index);
        value = l;
    }

    public LongInfo(DataInputStream in, int index) throws IOException
    {
        super(index);
        value = in.readLong();
    }

    @Override
    public int hashCode() { return (int)(value ^ (value >>> 32)); }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LongInfo && ((LongInfo)obj).value == value;
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<String,String> map)
    {
        return dest.addLongInfo(value);
    }

    @Override
    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(tag);
        out.writeLong(value);
    }

    @Override
    public void print(PrintWriter out)
    {
        out.print("Long ");
        out.println(value);
    }
}

class DoubleInfo extends ConstInfo
{
    static final int tag = 6;
    double value;

    public DoubleInfo(double d, int index)
    {
        super(index);
        value = d;
    }

    public DoubleInfo(DataInputStream in, int index) throws IOException
    {
        super(index);
        value = in.readDouble();
    }

    @Override
    public int hashCode() {
        long v = Double.doubleToLongBits(value);
        return (int)(v ^ (v >>> 32));
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof DoubleInfo
                && ((DoubleInfo)obj).value == value;
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<String,String> map)
    {
        return dest.addDoubleInfo(value);
    }

    @Override
    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(tag);
        out.writeDouble(value);
    }

    @Override
    public void print(PrintWriter out)
    {
        out.print("Double ");
        out.println(value);
    }
}

class Utf8Info extends ConstInfo
{
    static final int tag = 1;
    String string;

    public Utf8Info(String utf8, int index)
    {
        super(index);
        string = utf8;
    }

    public Utf8Info(DataInputStream in, int index)
            throws IOException
    {
        super(index);
        string = in.readUTF();
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Utf8Info
                && ((Utf8Info)obj).string.equals(string);
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public int copy(ConstPool src, ConstPool dest,
            Map<String,String> map)
    {
        return dest.addUtf8Info(string);
    }

    @Override
    public void write(DataOutputStream out)
            throws IOException
    {
        out.writeByte(tag);
        out.writeUTF(string);
    }

    @Override
    public void print(PrintWriter out) {
        out.print("UTF8 \"");
        out.print(string);
        out.println("\"");
    }
}

class MethodHandleInfo extends ConstInfo {
    static final int tag = 15;
    int refKind, refIndex;

    public MethodHandleInfo(int kind, int referenceIndex, int index) {
        super(index);
        refKind = kind;
        refIndex = referenceIndex;
    }

    public MethodHandleInfo(DataInputStream in, int index)
            throws IOException
    {
        super(index);
        refKind = in.readUnsignedByte();
        refIndex = in.readUnsignedShort();
    }

    @Override
    public int hashCode() { return (refKind << 16) ^ refIndex; }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof MethodHandleInfo) {
            MethodHandleInfo mh = (MethodHandleInfo)obj;
            return mh.refKind == refKind && mh.refIndex == refIndex;
        }
        return false;
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public int copy(ConstPool src, ConstPool dest,
            Map<String,String> map)
    {
       return dest.addMethodHandleInfo(refKind,
                   src.getItem(refIndex).copy(src, dest, map));
    }

    @Override
    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(tag);
        out.writeByte(refKind);
        out.writeShort(refIndex);
    }

    @Override
    public void print(PrintWriter out) {
        out.print("MethodHandle #");
        out.print(refKind);
        out.print(", index #");
        out.println(refIndex);
    }
}

class MethodTypeInfo extends ConstInfo
{
    static final int tag = 16;
    int descriptor;

    public MethodTypeInfo(int desc, int index)
    {
        super(index);
        descriptor = desc;
    }

    public MethodTypeInfo(DataInputStream in, int index)
            throws IOException
    {
        super(index);
        descriptor = in.readUnsignedShort();
    }

    @Override
    public int hashCode() { return descriptor; }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof MethodTypeInfo)
            return ((MethodTypeInfo)obj).descriptor == descriptor;
        return false;
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public void renameClass(ConstPool cp, String oldName, String newName,
            Map<ConstInfo,ConstInfo> cache)
    {
        String desc = cp.getUtf8Info(descriptor);
        String desc2 = Descriptor.rename(desc, oldName, newName);
        if (desc != desc2)
            if (cache == null)
                descriptor = cp.addUtf8Info(desc2);
            else {
                cache.remove(this);
                descriptor = cp.addUtf8Info(desc2);
                cache.put(this, this);
            }
    }

    @Override
    public void renameClass(ConstPool cp, Map<String,String> map,
            Map<ConstInfo,ConstInfo> cache)
    {
        String desc = cp.getUtf8Info(descriptor);
        String desc2 = Descriptor.rename(desc, map);
        if (desc != desc2)
            if (cache == null)
                descriptor = cp.addUtf8Info(desc2);
            else {
                cache.remove(this);
                descriptor = cp.addUtf8Info(desc2);
                cache.put(this, this);
            }
    }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<String,String> map)
    {
        String desc = src.getUtf8Info(descriptor);
        desc = Descriptor.rename(desc, map);
        return dest.addMethodTypeInfo(dest.addUtf8Info(desc));
    }

    @Override
    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(tag);
        out.writeShort(descriptor);
    }

    @Override
    public void print(PrintWriter out) {
        out.print("MethodType #");
        out.println(descriptor);
    }
}

class InvokeDynamicInfo extends ConstInfo
{
    static final int tag = 18;
    int bootstrap, nameAndType;

    public InvokeDynamicInfo(int bootstrapMethod,
            int ntIndex, int index)
    {
        super(index);
        bootstrap = bootstrapMethod;
        nameAndType = ntIndex;
    }

    public InvokeDynamicInfo(DataInputStream in, int index)
            throws IOException
    {
        super(index);
        bootstrap = in.readUnsignedShort();
        nameAndType = in.readUnsignedShort();
    }

    @Override
    public int hashCode() { return (bootstrap << 16) ^ nameAndType; }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof InvokeDynamicInfo) {
            InvokeDynamicInfo iv = (InvokeDynamicInfo)obj;
            return iv.bootstrap == bootstrap
                    && iv.nameAndType == nameAndType;
        }
        return false;
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public int copy(ConstPool src, ConstPool dest,
            Map<String,String> map)
    {
       return dest.addInvokeDynamicInfo(bootstrap,
            src.getItem(nameAndType).copy(src, dest, map));
    }

    @Override
    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(tag);
        out.writeShort(bootstrap);
        out.writeShort(nameAndType);
    }

    @Override
    public void print(PrintWriter out) {
        out.print("InvokeDynamic #");
        out.print(bootstrap);
        out.print(", name&type #");
        out.println(nameAndType);
    }
}

class ModuleInfo extends ConstInfo
{
    static final int tag = 19;
    int name;

    public ModuleInfo(int moduleName, int index)
    {
        super(index);
        name = moduleName;
    }

    public ModuleInfo(DataInputStream in, int index)
            throws IOException
    {
        super(index);
        name = in.readUnsignedShort();
    }

    @Override
    public int hashCode() { return name; }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof ModuleInfo
                && ((ModuleInfo)obj).name == name;
    }

    @Override
    public int getTag() { return tag; }

    public String getModuleName(ConstPool cp)
    {
        return cp.getUtf8Info(name);
    }

    @Override
    public int copy(ConstPool src, ConstPool dest,
            Map<String,String> map)
    {
        String moduleName = src.getUtf8Info(name);
        int newName = dest.addUtf8Info(moduleName);
        return dest.addModuleInfo(newName);
    }

    @Override
    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(tag);
        out.writeShort(name);
    }

    @Override
    public void print(PrintWriter out) {
        out.print("Module #");
        out.println(name);
    }
}

class PackageInfo extends ConstInfo
{
    static final int tag = 20;
    int name;

    public PackageInfo(int moduleName, int index)
    {
        super(index);
        name = moduleName;
    }

    public PackageInfo(DataInputStream in, int index)
            throws IOException
    {
        super(index);
        name = in.readUnsignedShort();
    }

    @Override
    public int hashCode() { return name; }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PackageInfo
                && ((PackageInfo)obj).name == name;
    }

    @Override
    public int getTag() { return tag; }

    public String getPackageName(ConstPool cp)
    {
        return cp.getUtf8Info(name);
    }

    @Override
    public int copy(ConstPool src, ConstPool dest,
            Map<String,String> map)
    {
        String packageName = src.getUtf8Info(name);
        int newName = dest.addUtf8Info(packageName);
        return dest.addModuleInfo(newName);
    }

    @Override
    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(tag);
        out.writeShort(name);
    }

    @Override
    public void print(PrintWriter out)
    {
        out.print("Package #");
        out.println(name);
    }
}
