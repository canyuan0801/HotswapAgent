

package org.hotswap.agent.javassist.convert;

import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtField;
import org.hotswap.agent.javassist.Modifier;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;

final public class TransformFieldAccess extends Transformer {
    private String newClassname, newFieldname;
    private String fieldname;
    private CtClass fieldClass;
    private boolean isPrivate;


    private int newIndex;
    private ConstPool constPool;

    public TransformFieldAccess(Transformer next, CtField field,
                                String newClassname, String newFieldname)
    {
        super(next);
        this.fieldClass = field.getDeclaringClass();
        this.fieldname = field.getName();
        this.isPrivate = Modifier.isPrivate(field.getModifiers());
        this.newClassname = newClassname;
        this.newFieldname = newFieldname;
        this.constPool = null;
    }

    @Override
    public void initialize(ConstPool cp, CodeAttribute attr) {
        if (constPool != cp)
            newIndex = 0;
    }


    @Override
    public int transform(CtClass clazz, int pos,
                         CodeIterator iterator, ConstPool cp)
    {
        int c = iterator.byteAt(pos);
        if (c == GETFIELD || c == GETSTATIC
                                || c == PUTFIELD || c == PUTSTATIC) {
            int index = iterator.u16bitAt(pos + 1);
            String typedesc
                = TransformReadField.isField(clazz.getClassPool(), cp,
                                fieldClass, fieldname, isPrivate, index);
            if (typedesc != null) {
                if (newIndex == 0) {
                    int nt = cp.addNameAndTypeInfo(newFieldname,
                                                   typedesc);
                    newIndex = cp.addFieldrefInfo(
                                        cp.addClassInfo(newClassname), nt);
                    constPool = cp;
                }

                iterator.write16bit(newIndex, pos + 1);
            }
        }

        return pos;
    }
}
