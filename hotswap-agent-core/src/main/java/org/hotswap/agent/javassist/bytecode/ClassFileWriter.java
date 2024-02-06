

package org.hotswap.agent.javassist.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class ClassFileWriter {
    private ByteStream output;
    private ConstPoolWriter constPool;
    private FieldWriter fields;
    private MethodWriter methods;
    int thisClass, superClass;


    public ClassFileWriter(int major, int minor) {
        output = new ByteStream(512);
        output.writeInt(0xCAFEBABE);
        output.writeShort(minor);
        output.writeShort(major);
        constPool = new ConstPoolWriter(output);
        fields = new FieldWriter(constPool);
        methods = new MethodWriter(constPool);

    }


    public ConstPoolWriter getConstPool() { return constPool; }


    public FieldWriter getFieldWriter() { return fields; }


    public MethodWriter getMethodWriter() { return methods; }


    public byte[] end(int accessFlags, int thisClass, int superClass,
                      int[] interfaces, AttributeWriter aw) {
        constPool.end();
        output.writeShort(accessFlags);
        output.writeShort(thisClass);
        output.writeShort(superClass);
        if (interfaces == null)
            output.writeShort(0);
        else {
            int n = interfaces.length;
            output.writeShort(n);
            for (int i = 0; i < n; i++)
                output.writeShort(interfaces[i]);
        }

        output.enlarge(fields.dataSize() + methods.dataSize() + 6);
        try {
            output.writeShort(fields.size());
            fields.write(output);

            output.writeShort(methods.numOfMethods());
            methods.write(output);
        }
        catch (IOException e) {}

        writeAttribute(output, aw, 0);
        return output.toByteArray();
    }


    public void end(DataOutputStream out,
                    int accessFlags, int thisClass, int superClass,
                    int[] interfaces, AttributeWriter aw)
        throws IOException
    {
        constPool.end();
        output.writeTo(out);
        out.writeShort(accessFlags);
        out.writeShort(thisClass);
        out.writeShort(superClass);
        if (interfaces == null)
            out.writeShort(0);
        else {
            int n = interfaces.length;
            out.writeShort(n);
            for (int i = 0; i < n; i++)
                out.writeShort(interfaces[i]);
        }

        out.writeShort(fields.size());
        fields.write(out);

        out.writeShort(methods.numOfMethods());
        methods.write(out);
        if (aw == null)
            out.writeShort(0);
        else {
            out.writeShort(aw.size());
            aw.write(out);
        }
    }


    public static interface AttributeWriter {

        public int size();


        public void write(DataOutputStream out) throws IOException;
    }

    static void writeAttribute(ByteStream bs, AttributeWriter aw, int attrCount) {
        if (aw == null) {
            bs.writeShort(attrCount);
            return;
        }

        bs.writeShort(aw.size() + attrCount);
        DataOutputStream dos = new DataOutputStream(bs);
        try {
            aw.write(dos);
            dos.flush();
        }
        catch (IOException e) {}
    }


    public static final class FieldWriter {
        protected ByteStream output;
        protected ConstPoolWriter constPool;
        private int fieldCount;

        FieldWriter(ConstPoolWriter cp) {
            output = new ByteStream(128);
            constPool = cp;
            fieldCount = 0;
        }


        public void add(int accessFlags, String name, String descriptor, AttributeWriter aw) {
            int nameIndex = constPool.addUtf8Info(name);
            int descIndex = constPool.addUtf8Info(descriptor);
            add(accessFlags, nameIndex, descIndex, aw);
        }


        public void add(int accessFlags, int name, int descriptor, AttributeWriter aw) {
            ++fieldCount;
            output.writeShort(accessFlags);
            output.writeShort(name);
            output.writeShort(descriptor);
            writeAttribute(output, aw, 0);
        }

        int size() { return fieldCount; }

        int dataSize() { return output.size(); }


        void write(OutputStream out) throws IOException {
            output.writeTo(out);
        }
    }


    public static final class MethodWriter {
        protected ByteStream output;
        protected ConstPoolWriter constPool;
        private int methodCount;
        protected int codeIndex;
        protected int throwsIndex;
        protected int stackIndex;

        private int startPos;
        private boolean isAbstract;
        private int catchPos;
        private int catchCount;

        MethodWriter(ConstPoolWriter cp) {
            output = new ByteStream(256);
            constPool = cp;
            methodCount = 0;
            codeIndex = 0;
            throwsIndex = 0;
            stackIndex = 0;
        }


        public void begin(int accessFlags, String name, String descriptor,
                        String[] exceptions, AttributeWriter aw) {
            int nameIndex = constPool.addUtf8Info(name);
            int descIndex = constPool.addUtf8Info(descriptor);
            int[] intfs;
            if (exceptions == null)
                intfs = null;
            else
                intfs = constPool.addClassInfo(exceptions);

            begin(accessFlags, nameIndex, descIndex, intfs, aw);
        }


        public void begin(int accessFlags, int name, int descriptor, int[] exceptions, AttributeWriter aw) {
            ++methodCount;
            output.writeShort(accessFlags);
            output.writeShort(name);
            output.writeShort(descriptor);
            isAbstract = (accessFlags & AccessFlag.ABSTRACT) != 0;

            int attrCount = isAbstract ? 0 : 1;
            if (exceptions != null)
                ++attrCount;

            writeAttribute(output, aw, attrCount);

            if (exceptions != null)
                writeThrows(exceptions);

            if (!isAbstract) {
                if (codeIndex == 0)
                    codeIndex = constPool.addUtf8Info(CodeAttribute.tag);

                startPos = output.getPos();
                output.writeShort(codeIndex);
                output.writeBlank(12);
            }

            catchPos = -1;
            catchCount = 0;
        }

        private void writeThrows(int[] exceptions) {
            if (throwsIndex == 0)
                throwsIndex = constPool.addUtf8Info(ExceptionsAttribute.tag);

            output.writeShort(throwsIndex);
            output.writeInt(exceptions.length * 2 + 2);
            output.writeShort(exceptions.length);
            for (int i = 0; i < exceptions.length; i++)
                output.writeShort(exceptions[i]);
        }


        public void add(int b) {
            output.write(b);
        }


        public void add16(int b) {
            output.writeShort(b);
        }


        public void add32(int b) {
            output.writeInt(b);
        }


        public void addInvoke(int opcode, String targetClass, String methodName,
                              String descriptor) {
            int target = constPool.addClassInfo(targetClass);
            int nt = constPool.addNameAndTypeInfo(methodName, descriptor);
            int method = constPool.addMethodrefInfo(target, nt);
            add(opcode);
            add16(method);
        }


        public void codeEnd(int maxStack, int maxLocals) {
            if (!isAbstract) {
                output.writeShort(startPos + 6, maxStack);
                output.writeShort(startPos + 8, maxLocals);
                output.writeInt(startPos + 10, output.getPos() - startPos - 14);
                catchPos = output.getPos();
                catchCount = 0;
                output.writeShort(0);
            }
        }


        public void addCatch(int startPc, int endPc, int handlerPc, int catchType) {
            ++catchCount;
            output.writeShort(startPc);
            output.writeShort(endPc);
            output.writeShort(handlerPc);
            output.writeShort(catchType);
        }


        public void end(StackMapTable.Writer smap, AttributeWriter aw) {
            if (isAbstract)
                return;


            output.writeShort(catchPos, catchCount);

            int attrCount = smap == null ? 0 : 1;
            writeAttribute(output, aw, attrCount);

            if (smap != null) {
                if (stackIndex == 0)
                    stackIndex = constPool.addUtf8Info(StackMapTable.tag);

                output.writeShort(stackIndex);
                byte[] data = smap.toByteArray();
                output.writeInt(data.length);
                output.write(data);
            }


            output.writeInt(startPos + 2, output.getPos() - startPos - 6);
        }


        public int size() { return output.getPos() - startPos - 14; } 

        int numOfMethods() { return methodCount; }

        int dataSize() { return output.size(); }


        void write(OutputStream out) throws IOException {
            output.writeTo(out);
        }
    }


    public static final class ConstPoolWriter {
        ByteStream output;
        protected int startPos;
        protected int num;

        ConstPoolWriter(ByteStream out) {
            output = out;
            startPos = out.getPos();
            num = 1;
            output.writeShort(1);
        }


        public int[] addClassInfo(String[] classNames) {
            int n = classNames.length;
            int[] result = new int[n];
            for (int i = 0; i < n; i++)
                result[i] = addClassInfo(classNames[i]);

            return result;
        }


        public int addClassInfo(String jvmname) {
            int utf8 = addUtf8Info(jvmname);
            output.write(ClassInfo.tag);
            output.writeShort(utf8);
            return num++;
        }


        public int addClassInfo(int name) {
            output.write(ClassInfo.tag);
            output.writeShort(name);
            return num++;
        }


        public int addNameAndTypeInfo(String name, String type) {
            return addNameAndTypeInfo(addUtf8Info(name), addUtf8Info(type));
        }


        public int addNameAndTypeInfo(int name, int type) {
            output.write(NameAndTypeInfo.tag);
            output.writeShort(name);
            output.writeShort(type);
            return num++;
        }


        public int addFieldrefInfo(int classInfo, int nameAndTypeInfo) {
            output.write(FieldrefInfo.tag);
            output.writeShort(classInfo);
            output.writeShort(nameAndTypeInfo);
            return num++;
        }


        public int addMethodrefInfo(int classInfo, int nameAndTypeInfo) {
            output.write(MethodrefInfo.tag);
            output.writeShort(classInfo);
            output.writeShort(nameAndTypeInfo);
            return num++;
        }


        public int addInterfaceMethodrefInfo(int classInfo,
                                             int nameAndTypeInfo) {
            output.write(InterfaceMethodrefInfo.tag);
            output.writeShort(classInfo);
            output.writeShort(nameAndTypeInfo);
            return num++;
        }


        public int addMethodHandleInfo(int kind, int index) {
            output.write(MethodHandleInfo.tag);
            output.write(kind);
            output.writeShort(index);
            return num++;
        }


        public int addMethodTypeInfo(int desc) {
            output.write(MethodTypeInfo.tag);
            output.writeShort(desc);
            return num++;
        }


        public int addInvokeDynamicInfo(int bootstrap,
                                        int nameAndTypeInfo) {
            output.write(InvokeDynamicInfo.tag);
            output.writeShort(bootstrap);
            output.writeShort(nameAndTypeInfo);
            return num++;
        }


        public int addStringInfo(String str) {
            int utf8 = addUtf8Info(str);
            output.write(StringInfo.tag);
            output.writeShort(utf8);
            return num++;
        }


        public int addIntegerInfo(int i) {
            output.write(IntegerInfo.tag);
            output.writeInt(i);
            return num++;
        }


        public int addFloatInfo(float f) {
            output.write(FloatInfo.tag);
            output.writeFloat(f);
            return num++;
        }


        public int addLongInfo(long l) {
            output.write(LongInfo.tag);
            output.writeLong(l);
            int n = num;
            num += 2;
            return n;
        }


        public int addDoubleInfo(double d) {
            output.write(DoubleInfo.tag);
            output.writeDouble(d);
            int n = num;
            num += 2;
            return n;
        }


        public int addUtf8Info(String utf8) {
            output.write(Utf8Info.tag);
            output.writeUTF(utf8);
            return num++;
        }


        void end() {
            output.writeShort(startPos, num);
        }
    }
}
