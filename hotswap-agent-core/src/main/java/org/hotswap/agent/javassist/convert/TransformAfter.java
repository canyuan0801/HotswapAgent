

package org.hotswap.agent.javassist.convert;

import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.CodeIterator;

public class TransformAfter extends TransformBefore {
    public TransformAfter(Transformer next,
                           CtMethod origMethod, CtMethod afterMethod)
        throws NotFoundException
    {
        super(next, origMethod, afterMethod);
    }

    @Override
    protected int match2(int pos, CodeIterator iterator) throws BadBytecode {
        iterator.move(pos);
        iterator.insert(saveCode);
        iterator.insert(loadCode);
        int p = iterator.insertGap(3);
        iterator.setMark(p);
        iterator.insert(loadCode);
        pos = iterator.next();
        p = iterator.getMark();
        iterator.writeByte(iterator.byteAt(pos), p);
        iterator.write16bit(iterator.u16bitAt(pos + 1), p + 1);
        iterator.writeByte(INVOKESTATIC, pos);
        iterator.write16bit(newIndex, pos + 1);
        iterator.move(p);
        return iterator.next();
    }
}
