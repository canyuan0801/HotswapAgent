
package org.hotswap.agent.plugin.proxy;

import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingAnnotationAdapter;
import org.objectweb.asm.commons.RemappingMethodAdapter;


public class TestClassAdapter extends ClassRemapper {

    private final static String METHOD_SUFFIX = "___";

    private boolean isObject;

    public TestClassAdapter(ClassVisitor cv,
            final Map<String, String> typeMappings) {
        super(cv, new Remapper() {
            @Override
            public String map(String type) {
                return typeMappings.containsKey(type) ? typeMappings.get(type)
                        : type;
            }
        });
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {

        String newName = remapper.mapType(name);
        if (newName.equals("java/lang/Object")) {
            superName = null;
            isObject = true;
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        return super.visitMethod(access, stripMethodSuffix(name), desc,
                signature, exceptions);
    }


    public String getClassName() {
        return remapper.mapType(className);
    }

    protected MethodVisitor createRemappingMethodAdapter(int access,
            String newDesc, MethodVisitor mv) {
        return new RemappingMethodAdapter(access, newDesc, mv, remapper) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name,
                    String desc, boolean itf) {
                if (name.equals("<init>") && isObject
                        && owner.equals("java/lang/Object")) {
                    return;
                }

                super.visitMethodInsn(opcode, owner, stripMethodSuffix(name),
                        desc, itf);
            }
        };
    }

    private static String stripMethodSuffix(String name) {
        int pos = name.indexOf(METHOD_SUFFIX);
        return (pos != -1) ? name.substring(0, pos) : name;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc,
            final boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(remapper.mapDesc(desc),
                visible);
        return av == null ? null
                : new RemappingAnnotationAdapter(av, remapper) {
                    @Override
                    public void visitEnum(String name, String enumDesc,
                            String value) {
                        if (Type.getType(enumDesc).getClassName()
                                .equals(RedefinitionPolicy.class.getName())) {
                            RedefinitionPolicy valueAsEnum = RedefinitionPolicy
                                    .valueOf(value);
                            if (Type.getType(desc).getClassName().equals(
                                    FieldRedefinitionPolicy.class.getName())) {
                                cv.visitAttribute(new SingleByteAttribute(
                                        FieldRedefinitionPolicy.class
                                                .getSimpleName(),
                                        (byte) valueAsEnum.ordinal()));
                            }
                            if (Type.getType(desc).getClassName().equals(
                                    MethodRedefinitionPolicy.class.getName())) {
                                cv.visitAttribute(new SingleByteAttribute(
                                        MethodRedefinitionPolicy.class
                                                .getSimpleName(),
                                        (byte) valueAsEnum.ordinal()));
                            }
                        }
                        super.visitEnum(name, desc, value);
                    }
                };
    }

    private static class SingleByteAttribute extends Attribute {
        private byte value;

        public SingleByteAttribute(String name, byte value) {
            super(name);
            this.value = value;
        }

        @Override
        protected ByteVector write(ClassWriter writer, byte[] code, int len,
                int maxStack, int maxLocals) {
            return new ByteVector().putByte(value);
        }
    }

}
