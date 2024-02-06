

package org.hotswap.agent.javassist.convert;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;

final public class TransformNewClass extends Transformer {
    private int nested;
    private String classname, newClassName;
    private int newClassIndex, newMethodNTIndex, newMethodIndex;

    public TransformNewClass(Transformer next,
                             String classname, String newClassName) {
        super(next);
        this.classname = classname;
        this.newClassName = newClassName;
    }

    @Override
    public void initialize(ConstPool cp, CodeAttribute attr) {
        nested = 0;
        newClassIndex = newMethodNTIndex = newMethodIndex = 0;
    }


    @Override
    public int transform(CtClass clazz, int pos, CodeIterator iterator,
                         ConstPool cp) throws CannotCompileException
    {
        int index;
        int c = iterator.byteAt(pos);
        if (c == NEW) {
            index = iterator.u16bitAt(pos + 1);
            if (cp.getClassInfo(index).equals(classname)) {
                if (iterator.byteAt(pos + 3) != DUP)
                    throw new CannotCompileException(
                                "NEW followed by no DUP was found");

                if (newClassIndex == 0)
                    newClassIndex = cp.addClassInfo(newClassName);

                iterator.write16bit(newClassIndex, pos + 1);
                ++nested;
            }
        }
        else if (c == INVOKESPECIAL) {
            index = iterator.u16bitAt(pos + 1);
            int typedesc = cp.isConstructor(classname, index);
            if (typedesc != 0 && nested > 0) {
                int nt = cp.getMethodrefNameAndType(index);
                if (newMethodNTIndex != nt) {
                    newMethodNTIndex = nt;
                    newMethodIndex = cp.addMethodrefInfo(newClassIndex, nt);
                }

                iterator.write16bit(newMethodIndex, pos + 1);
                --nested;
            }
        }

        return pos;
    }
}
