

package org.hotswap.agent.javassist.convert;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtField;
import org.hotswap.agent.javassist.Modifier;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;

public class TransformReadField extends Transformer {
    protected String fieldname;
    protected CtClass fieldClass;
    protected boolean isPrivate;
    protected String methodClassname, methodName;

    public TransformReadField(Transformer next, CtField field,
                              String methodClassname, String methodName)
    {
        super(next);
        this.fieldClass = field.getDeclaringClass();
        this.fieldname = field.getName();
        this.methodClassname = methodClassname;
        this.methodName = methodName;
        this.isPrivate = Modifier.isPrivate(field.getModifiers());
    }

    static String isField(ClassPool pool, ConstPool cp, CtClass fclass,
                          String fname, boolean is_private, int index) {
        if (!cp.getFieldrefName(index).equals(fname))
            return null;

        try {
            CtClass c = pool.get(cp.getFieldrefClassName(index));
            if (c == fclass || (!is_private && isFieldInSuper(c, fclass, fname)))
                return cp.getFieldrefType(index);
        }
        catch (NotFoundException e) {}
        return null;
    }

    static boolean isFieldInSuper(CtClass clazz, CtClass fclass, String fname) {
        if (!clazz.subclassOf(fclass))
            return false;

        try {
            CtField f = clazz.getField(fname);
            return f.getDeclaringClass() == fclass;
        }
        catch (NotFoundException e) {}
        return false;
    }

    @Override
    public int transform(CtClass tclazz, int pos, CodeIterator iterator,
                         ConstPool cp) throws BadBytecode
    {
        int c = iterator.byteAt(pos);
        if (c == GETFIELD || c == GETSTATIC) {
            int index = iterator.u16bitAt(pos + 1);
            String typedesc = isField(tclazz.getClassPool(), cp,
                                fieldClass, fieldname, isPrivate, index);
            if (typedesc != null) {
                if (c == GETSTATIC) {
                    iterator.move(pos);
                    pos = iterator.insertGap(1);
                    iterator.writeByte(ACONST_NULL, pos);
                    pos = iterator.next();
                }

                String type = "(Ljava/lang/Object;)" + typedesc;
                int mi = cp.addClassInfo(methodClassname);
                int methodref = cp.addMethodrefInfo(mi, methodName, type);
                iterator.writeByte(INVOKESTATIC, pos);
                iterator.write16bit(methodref, pos + 1);
                return pos;
            }
        }

        return pos;
    }
}
