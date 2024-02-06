
package org.hotswap.agent.javassist.bytecode.analysis;

import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.Opcode;


public class Util implements Opcode {
    public static int getJumpTarget(int pos, CodeIterator iter) {
        int opcode = iter.byteAt(pos);
        pos += (opcode == JSR_W || opcode == GOTO_W) ? iter.s32bitAt(pos + 1) : iter.s16bitAt(pos + 1);
        return pos;
    }

    public static boolean isJumpInstruction(int opcode) {
        return (opcode >= IFEQ && opcode <= JSR) || opcode == IFNULL || opcode == IFNONNULL || opcode == JSR_W || opcode == GOTO_W;
    }

    public static boolean isGoto(int opcode) {
        return opcode == GOTO || opcode == GOTO_W;
    }

    public static boolean isJsr(int opcode) {
        return opcode == JSR || opcode == JSR_W;
    }

    public static boolean isReturn(int opcode) {
        return (opcode >= IRETURN && opcode <= RETURN);
    }
}
