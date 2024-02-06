

package org.hotswap.agent.javassist.convert;

import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtField;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;

final public class TransformWriteField extends TransformReadField {
    public TransformWriteField(Transformer next, CtField field,
                               String methodClassname, String methodName)
    {
        super(next, field, methodClassname, methodName);
    }

    @Override
    public int transform(CtClass tclazz, int pos, CodeIterator iterator,
                         ConstPool cp) throws BadBytecode
    {
        int c = iterator.byteAt(pos);
        if (c == PUTFIELD || c == PUTSTATIC) {
            int index = iterator.u16bitAt(pos + 1);
            String typedesc = isField(tclazz.getClassPool(), cp,
                                fieldClass, fieldname, isPrivate, index);
            if (typedesc != null) {
                if (c == PUTSTATIC) {
                    CodeAttribute ca = iterator.get();
                    iterator.move(pos);
                    char c0 = typedesc.charAt(0);
                    if (c0 == 'J' || c0 == 'D') {

                        pos = iterator.insertGap(3);
                        iterator.writeByte(ACONST_NULL, pos);
                        iterator.writeByte(DUP_X2, pos + 1);
                        iterator.writeByte(POP, pos + 2);
                        ca.setMaxStack(ca.getMaxStack() + 2);
                    }
                    else {

                        pos = iterator.insertGap(2);
                        iterator.writeByte(ACONST_NULL, pos);
                        iterator.writeByte(SWAP, pos + 1);
                        ca.setMaxStack(ca.getMaxStack() + 1);
                    }

                    pos = iterator.next();
                }

                int mi = cp.addClassInfo(methodClassname);
                String type = "(Ljava/lang/Object;" + typedesc + ")V";
                int methodref = cp.addMethodrefInfo(mi, methodName, type);
                iterator.writeByte(INVOKESTATIC, pos);
                iterator.write16bit(methodref, pos + 1);
            }
        }

        return pos;
    }
}
