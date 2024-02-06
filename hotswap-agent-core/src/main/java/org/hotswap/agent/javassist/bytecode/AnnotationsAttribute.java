

package org.hotswap.agent.javassist.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hotswap.agent.javassist.bytecode.annotation.Annotation;
import org.hotswap.agent.javassist.bytecode.annotation.AnnotationMemberValue;
import org.hotswap.agent.javassist.bytecode.annotation.AnnotationsWriter;
import org.hotswap.agent.javassist.bytecode.annotation.ArrayMemberValue;
import org.hotswap.agent.javassist.bytecode.annotation.BooleanMemberValue;
import org.hotswap.agent.javassist.bytecode.annotation.ByteMemberValue;
import org.hotswap.agent.javassist.bytecode.annotation.CharMemberValue;
import org.hotswap.agent.javassist.bytecode.annotation.ClassMemberValue;
import org.hotswap.agent.javassist.bytecode.annotation.DoubleMemberValue;
import org.hotswap.agent.javassist.bytecode.annotation.EnumMemberValue;
import org.hotswap.agent.javassist.bytecode.annotation.FloatMemberValue;
import org.hotswap.agent.javassist.bytecode.annotation.IntegerMemberValue;
import org.hotswap.agent.javassist.bytecode.annotation.LongMemberValue;
import org.hotswap.agent.javassist.bytecode.annotation.MemberValue;
import org.hotswap.agent.javassist.bytecode.annotation.ShortMemberValue;
import org.hotswap.agent.javassist.bytecode.annotation.StringMemberValue;


public class AnnotationsAttribute extends AttributeInfo {

    public static final String visibleTag = "RuntimeVisibleAnnotations";


    public static final String invisibleTag = "RuntimeInvisibleAnnotations";


    public AnnotationsAttribute(ConstPool cp, String attrname, byte[] info) {
        super(cp, attrname, info);
    }


    public AnnotationsAttribute(ConstPool cp, String attrname) {
        this(cp, attrname, new byte[] { 0, 0 });
    }


    AnnotationsAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }


    public int numAnnotations() {
        return ByteArray.readU16bit(info, 0);
    }


    @Override
    public AttributeInfo copy(ConstPool newCp, Map<String,String> classnames) {
        Copier copier = new Copier(info, constPool, newCp, classnames);
        try {
            copier.annotationArray();
            return new AnnotationsAttribute(newCp, getName(), copier.close());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public Annotation getAnnotation(String type) {
        Annotation[] annotations = getAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].getTypeName().equals(type))
                return annotations[i];
        }

        return null;
    }


    public void addAnnotation(Annotation annotation) {
        String type = annotation.getTypeName();
        Annotation[] annotations = getAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].getTypeName().equals(type)) {
                annotations[i] = annotation;
                setAnnotations(annotations);
                return;
            }
        }

        Annotation[] newlist = new Annotation[annotations.length + 1];
        System.arraycopy(annotations, 0, newlist, 0, annotations.length);
        newlist[annotations.length] = annotation;
        setAnnotations(newlist);
    }


    public boolean removeAnnotation(String type) {
        Annotation[] annotations = getAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].getTypeName().equals(type)) {
                Annotation[] newlist = new Annotation[annotations.length - 1];
                System.arraycopy(annotations, 0, newlist, 0, i);
                if (i < annotations.length - 1) {
                    System.arraycopy(annotations, i + 1, newlist, i,
                                     annotations.length - i - 1);
                }
                setAnnotations(newlist);
                return true;
            }
        }
        return false;
    }


    public Annotation[] getAnnotations() {
        try {
            return new Parser(info, constPool).parseAnnotations();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void setAnnotations(Annotation[] annotations) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AnnotationsWriter writer = new AnnotationsWriter(output, constPool);
        try {
            int n = annotations.length;
            writer.numAnnotations(n);
            for (int i = 0; i < n; ++i)
                annotations[i].write(writer);

            writer.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        set(output.toByteArray());
    }


    public void setAnnotation(Annotation annotation) {
        setAnnotations(new Annotation[] { annotation });
    }


    @Override
    void renameClass(String oldname, String newname) {
        Map<String,String> map = new HashMap<String,String>();
        map.put(oldname, newname);
        renameClass(map);
    }

    @Override
    void renameClass(Map<String,String> classnames) {
        Renamer renamer = new Renamer(info, getConstPool(), classnames);
        try {
            renamer.annotationArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void getRefClasses(Map<String,String> classnames) { renameClass(classnames); }


    @Override
    public String toString() {
        Annotation[] a = getAnnotations();
        StringBuilder sbuf = new StringBuilder();
        int i = 0;
        while (i < a.length) {
            sbuf.append(a[i++].toString());
            if (i != a.length)
                sbuf.append(", ");
        }

        return sbuf.toString();
    }

    static class Walker {
        byte[] info;

        Walker(byte[] attrInfo) {
            info = attrInfo;
        }

        final void parameters() throws Exception {
            int numParam = info[0] & 0xff;
            parameters(numParam, 1);
        }

        void parameters(int numParam, int pos) throws Exception {
            for (int i = 0; i < numParam; ++i)
                pos = annotationArray(pos);
        }

        final void annotationArray() throws Exception {
            annotationArray(0);
        }

        final int annotationArray(int pos) throws Exception {
            int num = ByteArray.readU16bit(info, pos);
            return annotationArray(pos + 2, num);
        }

        int annotationArray(int pos, int num) throws Exception {
            for (int i = 0; i < num; ++i)
                pos = annotation(pos);

            return pos;
        }

        final int annotation(int pos) throws Exception {
            int type = ByteArray.readU16bit(info, pos);
            int numPairs = ByteArray.readU16bit(info, pos + 2);
            return annotation(pos + 4, type, numPairs);
        }

        int annotation(int pos, int type, int numPairs) throws Exception {
            for (int j = 0; j < numPairs; ++j)
                pos = memberValuePair(pos);

            return pos;
        }


        final int memberValuePair(int pos) throws Exception {
            int nameIndex = ByteArray.readU16bit(info, pos);
            return memberValuePair(pos + 2, nameIndex);
        }


        int memberValuePair(int pos, int nameIndex) throws Exception {
            return memberValue(pos);
        }


        final int memberValue(int pos) throws Exception {
            int tag = info[pos] & 0xff;
            if (tag == 'e') {
                int typeNameIndex = ByteArray.readU16bit(info, pos + 1);
                int constNameIndex = ByteArray.readU16bit(info, pos + 3);
                enumMemberValue(pos, typeNameIndex, constNameIndex);
                return pos + 5;
            }
            else if (tag == 'c') {
                int index = ByteArray.readU16bit(info, pos + 1);
                classMemberValue(pos, index);
                return pos + 3;
            }
            else if (tag == '@')
                return annotationMemberValue(pos + 1);
            else if (tag == '[') {
                int num = ByteArray.readU16bit(info, pos + 1);
                return arrayMemberValue(pos + 3, num);
            }
            else {
                int index = ByteArray.readU16bit(info, pos + 1);
                constValueMember(tag, index);
                return pos + 3;
            }
        }


        void constValueMember(int tag, int index) throws Exception {}


        void enumMemberValue(int pos, int typeNameIndex, int constNameIndex)
            throws Exception {
        }


        void classMemberValue(int pos, int index) throws Exception {}


        int annotationMemberValue(int pos) throws Exception {
            return annotation(pos);
        }


        int arrayMemberValue(int pos, int num) throws Exception {
            for (int i = 0; i < num; ++i) {
                pos = memberValue(pos);
            }

            return pos;
        }
    }

    static class Renamer extends Walker {
        ConstPool cpool;
        Map<String,String> classnames;


        Renamer(byte[] info, ConstPool cp, Map<String,String> map) {
            super(info);
            cpool = cp;
            classnames = map;
        }

        @Override
        int annotation(int pos, int type, int numPairs) throws Exception {
            renameType(pos - 4, type);
            return super.annotation(pos, type, numPairs);
        }

        @Override
        void enumMemberValue(int pos, int typeNameIndex, int constNameIndex)
            throws Exception
        {
            renameType(pos + 1, typeNameIndex);
            super.enumMemberValue(pos, typeNameIndex, constNameIndex);
        }

        @Override
        void classMemberValue(int pos, int index) throws Exception {
            renameType(pos + 1, index);
            super.classMemberValue(pos, index);
        }

        private void renameType(int pos, int index) {
            String name = cpool.getUtf8Info(index);
            String newName = Descriptor.rename(name, classnames);
            if (!name.equals(newName)) {
                int index2 = cpool.addUtf8Info(newName);
                ByteArray.write16bit(index2, info, pos);
            }
        }
    }

    static class Copier extends Walker {
        ByteArrayOutputStream output;
        AnnotationsWriter writer;
        ConstPool srcPool, destPool;
        Map<String,String> classnames;


        Copier(byte[] info, ConstPool src, ConstPool dest, Map<String,String> map) {
            this(info, src, dest, map, true); 
        }

        Copier(byte[] info, ConstPool src, ConstPool dest, Map<String,String> map, boolean makeWriter) {
            super(info);
            output = new ByteArrayOutputStream();
            if (makeWriter)
                writer = new AnnotationsWriter(output, dest);

            srcPool = src;
            destPool = dest;
            classnames = map;
        }

        byte[] close() throws IOException {
            writer.close();
            return output.toByteArray();
        }

        @Override
        void parameters(int numParam, int pos) throws Exception {
            writer.numParameters(numParam);
            super.parameters(numParam, pos);
        }

        @Override
        int annotationArray(int pos, int num) throws Exception {
            writer.numAnnotations(num);
            return super.annotationArray(pos, num);
        }

        @Override
        int annotation(int pos, int type, int numPairs) throws Exception {
            writer.annotation(copyType(type), numPairs);
            return super.annotation(pos, type, numPairs);
        }

        @Override
        int memberValuePair(int pos, int nameIndex) throws Exception {
            writer.memberValuePair(copy(nameIndex));
            return super.memberValuePair(pos, nameIndex);
        }

        @Override
        void constValueMember(int tag, int index) throws Exception {
            writer.constValueIndex(tag, copy(index));
            super.constValueMember(tag, index);
        }

        @Override
        void enumMemberValue(int pos, int typeNameIndex, int constNameIndex)
            throws Exception
        {
            writer.enumConstValue(copyType(typeNameIndex), copy(constNameIndex));
            super.enumMemberValue(pos, typeNameIndex, constNameIndex);
        }

        @Override
        void classMemberValue(int pos, int index) throws Exception {
            writer.classInfoIndex(copyType(index));
            super.classMemberValue(pos, index);
        }

        @Override
        int annotationMemberValue(int pos) throws Exception {
            writer.annotationValue();
            return super.annotationMemberValue(pos);
        }

        @Override
        int arrayMemberValue(int pos, int num) throws Exception {
            writer.arrayValue(num);
            return super.arrayMemberValue(pos, num);
        }


        int copy(int srcIndex) {
            return srcPool.copy(srcIndex, destPool, classnames);
        }


        int copyType(int srcIndex) {
            String name = srcPool.getUtf8Info(srcIndex);
            String newName = Descriptor.rename(name, classnames);
            return destPool.addUtf8Info(newName);
        }
    }

    static class Parser extends Walker {
        ConstPool pool;
        Annotation[][] allParams;
        Annotation[] allAnno;
        Annotation currentAnno;
        MemberValue currentMember;


        Parser(byte[] info, ConstPool cp) {
            super(info);
            pool = cp;
        }

        Annotation[][] parseParameters() throws Exception {
            parameters();
            return allParams;
        }

        Annotation[] parseAnnotations() throws Exception {
            annotationArray();
            return allAnno;
        }

        MemberValue parseMemberValue() throws Exception {
            memberValue(0);
            return currentMember;
        }

        @Override
        void parameters(int numParam, int pos) throws Exception {
            Annotation[][] params = new Annotation[numParam][];
            for (int i = 0; i < numParam; ++i) {
                pos = annotationArray(pos);
                params[i] = allAnno;
            }

            allParams = params;
        }

        @Override
        int annotationArray(int pos, int num) throws Exception {
            Annotation[] array = new Annotation[num];
            for (int i = 0; i < num; ++i) {
                pos = annotation(pos);
                array[i] = currentAnno;
            }

            allAnno = array;
            return pos;
        }

        @Override
        int annotation(int pos, int type, int numPairs) throws Exception {
            currentAnno = new Annotation(type, pool);
            return super.annotation(pos, type, numPairs);
        }

        @Override
        int memberValuePair(int pos, int nameIndex) throws Exception {
            pos = super.memberValuePair(pos, nameIndex);
            currentAnno.addMemberValue(nameIndex, currentMember);
            return pos;
        }

        @Override
        void constValueMember(int tag, int index) throws Exception {
            MemberValue m;
            ConstPool cp = pool;
            switch (tag) {
            case 'B' :
                m = new ByteMemberValue(index, cp);
                break;
            case 'C' :
                m = new CharMemberValue(index, cp);
                break;
            case 'D' :
                m = new DoubleMemberValue(index, cp);
                break;
            case 'F' :
                m = new FloatMemberValue(index, cp);
                break;
            case 'I' :
                m = new IntegerMemberValue(index, cp);
                break;
            case 'J' :
                m = new LongMemberValue(index, cp);
                break;
            case 'S' :
                m = new ShortMemberValue(index, cp);
                break;
            case 'Z' :
                m = new BooleanMemberValue(index, cp);
                break;
            case 's' :
                m = new StringMemberValue(index, cp);
                break;
            default :
                throw new RuntimeException("unknown tag:" + tag);
            }

            currentMember = m;
            super.constValueMember(tag, index);
        }

        @Override
        void enumMemberValue(int pos, int typeNameIndex, int constNameIndex)
            throws Exception
        {
            currentMember = new EnumMemberValue(typeNameIndex,
                                              constNameIndex, pool);
            super.enumMemberValue(pos, typeNameIndex, constNameIndex);
        }

        @Override
        void classMemberValue(int pos, int index) throws Exception {
            currentMember = new ClassMemberValue(index, pool);
            super.classMemberValue(pos, index);
        }

        @Override
        int annotationMemberValue(int pos) throws Exception {
            Annotation anno = currentAnno;
            pos = super.annotationMemberValue(pos);
            currentMember = new AnnotationMemberValue(currentAnno, pool);
            currentAnno = anno;
            return pos;
        }

        @Override
        int arrayMemberValue(int pos, int num) throws Exception {
            ArrayMemberValue amv = new ArrayMemberValue(pool);
            MemberValue[] elements = new MemberValue[num];
            for (int i = 0; i < num; ++i) {
                pos = memberValue(pos);
                elements[i] = currentMember;
            }

            amv.setValue(elements);
            currentMember = amv;
            return pos;
        }
    }
}
