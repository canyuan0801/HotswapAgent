

package org.hotswap.agent.javassist.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public final class FieldInfo {
    ConstPool constPool;
    int accessFlags;
    int name;
    String cachedName;
    String cachedType;
    int descriptor;
    List<AttributeInfo> attribute;

    private FieldInfo(ConstPool cp) {
        constPool = cp;
        accessFlags = 0;
        attribute = null;
    }


    public FieldInfo(ConstPool cp, String fieldName, String desc) {
        this(cp);
        name = cp.addUtf8Info(fieldName);
        cachedName = fieldName;
        descriptor = cp.addUtf8Info(desc);
    }

    FieldInfo(ConstPool cp, DataInputStream in) throws IOException {
        this(cp);
        read(in);
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

        AttributeInfo signature
            = getAttribute(SignatureAttribute.tag);
        if (signature != null) {
            signature = signature.copy(cp, null);
            newAttributes.add(signature);
        }

        int index = getConstantValue();
        if (index != 0) {
            index = constPool.copy(index, cp, null);
            newAttributes.add(new ConstantAttribute(cp, index));
        }

        attribute = newAttributes;
        name = cp.addUtf8Info(getName());
        descriptor = cp.addUtf8Info(getDescriptor());
        constPool = cp;
    }


    public ConstPool getConstPool() {
        return constPool;
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


    public int getConstantValue() {
        if ((accessFlags & AccessFlag.STATIC) == 0)
            return 0;

        ConstantAttribute attr
            = (ConstantAttribute)getAttribute(ConstantAttribute.tag);
        if (attr == null)
            return 0;
        return attr.getConstantValue();
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
