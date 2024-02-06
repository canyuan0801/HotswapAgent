
package org.hotswap.agent.javassist.convert;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.CodeConverter.ArrayAccessReplacementMethodNames;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.Descriptor;
import org.hotswap.agent.javassist.bytecode.MethodInfo;
import org.hotswap.agent.javassist.bytecode.analysis.Analyzer;
import org.hotswap.agent.javassist.bytecode.analysis.Frame;


public final class TransformAccessArrayField extends Transformer {
    private final String methodClassname;
    private final ArrayAccessReplacementMethodNames names;
    private Frame[] frames;
    private int offset;

    public TransformAccessArrayField(Transformer next, String methodClassname,
            ArrayAccessReplacementMethodNames names) throws NotFoundException {
        super(next);
        this.methodClassname = methodClassname;
        this.names = names;

    }

    @Override
    public void initialize(ConstPool cp, CtClass clazz, MethodInfo minfo) throws CannotCompileException {

        CodeIterator iterator = minfo.getCodeAttribute().iterator();
        while (iterator.hasNext()) {
            try {
                int pos = iterator.next();
                int c = iterator.byteAt(pos);

                if (c == AALOAD)
                    initFrames(clazz, minfo);

                if (c == AALOAD || c == BALOAD || c == CALOAD || c == DALOAD
                        || c == FALOAD || c == IALOAD || c == LALOAD
                        || c == SALOAD) {
                    pos = replace(cp, iterator, pos, c, getLoadReplacementSignature(c));
                } else if (c == AASTORE || c == BASTORE || c == CASTORE
                        || c == DASTORE || c == FASTORE || c == IASTORE
                        || c == LASTORE || c == SASTORE) {
                    pos = replace(cp, iterator, pos, c, getStoreReplacementSignature(c));
                }

            } catch (Exception e) {
                throw new CannotCompileException(e);
            }
        }
    }

    @Override
    public void clean() {
        frames = null;
        offset = -1;
    }

    @Override
    public int transform(CtClass tclazz, int pos, CodeIterator iterator,
            ConstPool cp) throws BadBytecode {

        return pos;
    }

    private Frame getFrame(int pos) throws BadBytecode {
        return frames[pos - offset];
    }

    private void initFrames(CtClass clazz, MethodInfo minfo) throws BadBytecode {
        if (frames == null) {
            frames = ((new Analyzer())).analyze(clazz, minfo);
            offset = 0;
        }
    }

    private int updatePos(int pos, int increment) {
        if (offset > -1)
            offset += increment;

        return pos + increment;
    }

    private String getTopType(int pos) throws BadBytecode {
        Frame frame = getFrame(pos);
        if (frame == null)
            return null;

        CtClass clazz = frame.peek().getCtClass();
        return clazz != null ? Descriptor.toJvmName(clazz) : null;
    }

    private int replace(ConstPool cp, CodeIterator iterator, int pos,
            int opcode, String signature) throws BadBytecode {
        String castType = null;
        String methodName = getMethodName(opcode);
        if (methodName != null) {

            if (opcode == AALOAD) {
                castType = getTopType(iterator.lookAhead());



                if (castType == null)
                    return pos;
                if ("java/lang/Object".equals(castType))
                    castType = null;
            }



            iterator.writeByte(NOP, pos);
            CodeIterator.Gap gap
                = iterator.insertGapAt(pos, castType != null ? 5 : 2, false);
            pos = gap.position;
            int mi = cp.addClassInfo(methodClassname);
            int methodref = cp.addMethodrefInfo(mi, methodName, signature);
            iterator.writeByte(INVOKESTATIC, pos);
            iterator.write16bit(methodref, pos + 1);

            if (castType != null) {
                int index = cp.addClassInfo(castType);
                iterator.writeByte(CHECKCAST, pos + 3);
                iterator.write16bit(index, pos + 4);
            }

            pos = updatePos(pos, gap.length);
        }

        return pos;
    }

    private String getMethodName(int opcode) {
        String methodName = null;
        switch (opcode) {
        case AALOAD:
            methodName = names.objectRead();
            break;
        case BALOAD:
            methodName = names.byteOrBooleanRead();
            break;
        case CALOAD:
            methodName = names.charRead();
            break;
        case DALOAD:
            methodName = names.doubleRead();
            break;
        case FALOAD:
            methodName = names.floatRead();
            break;
        case IALOAD:
            methodName = names.intRead();
            break;
        case SALOAD:
            methodName = names.shortRead();
            break;
        case LALOAD:
            methodName = names.longRead();
            break;
        case AASTORE:
            methodName = names.objectWrite();
            break;
        case BASTORE:
            methodName = names.byteOrBooleanWrite();
            break;
        case CASTORE:
            methodName = names.charWrite();
            break;
        case DASTORE:
            methodName = names.doubleWrite();
            break;
        case FASTORE:
            methodName = names.floatWrite();
            break;
        case IASTORE:
            methodName = names.intWrite();
            break;
        case SASTORE:
            methodName = names.shortWrite();
            break;
        case LASTORE:
            methodName = names.longWrite();
            break;
        }

        if (methodName.equals(""))
            methodName = null;

        return methodName;
    }

    private String getLoadReplacementSignature(int opcode) throws BadBytecode {
        switch (opcode) {
        case AALOAD:
            return "(Ljava/lang/Object;I)Ljava/lang/Object;";
        case BALOAD:
            return "(Ljava/lang/Object;I)B";
        case CALOAD:
            return "(Ljava/lang/Object;I)C";
        case DALOAD:
            return "(Ljava/lang/Object;I)D";
        case FALOAD:
            return "(Ljava/lang/Object;I)F";
        case IALOAD:
            return "(Ljava/lang/Object;I)I";
        case SALOAD:
            return "(Ljava/lang/Object;I)S";
        case LALOAD:
            return "(Ljava/lang/Object;I)J";
        }

        throw new BadBytecode(opcode);
    }

    private String getStoreReplacementSignature(int opcode) throws BadBytecode {
        switch (opcode) {
        case AASTORE:
            return "(Ljava/lang/Object;ILjava/lang/Object;)V";
        case BASTORE:
            return "(Ljava/lang/Object;IB)V";
        case CASTORE:
            return "(Ljava/lang/Object;IC)V";
        case DASTORE:
            return "(Ljava/lang/Object;ID)V";
        case FASTORE:
            return "(Ljava/lang/Object;IF)V";
        case IASTORE:
            return "(Ljava/lang/Object;II)V";
        case SASTORE:
            return "(Ljava/lang/Object;IS)V";
        case LASTORE:
            return "(Ljava/lang/Object;IJ)V";
        }

        throw new BadBytecode(opcode);
    }
}
