
package org.hotswap.agent.javassist.bytecode.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ExceptionTable;
import org.hotswap.agent.javassist.bytecode.MethodInfo;
import org.hotswap.agent.javassist.bytecode.Opcode;


public class SubroutineScanner implements Opcode {

    private Subroutine[] subroutines;
    Map<Integer,Subroutine> subTable = new HashMap<Integer,Subroutine>();
    Set<Integer> done = new HashSet<Integer>();


    public Subroutine[] scan(MethodInfo method) throws BadBytecode {
        CodeAttribute code = method.getCodeAttribute();
        CodeIterator iter = code.iterator();

        subroutines = new Subroutine[code.getCodeLength()];
        subTable.clear();
        done.clear();

        scan(0, iter, null);

        ExceptionTable exceptions = code.getExceptionTable();
        for (int i = 0; i < exceptions.size(); i++) {
            int handler = exceptions.handlerPc(i);


            scan(handler, iter, subroutines[exceptions.startPc(i)]);
        }

        return subroutines;
    }

    private void scan(int pos, CodeIterator iter, Subroutine sub) throws BadBytecode {

        if (done.contains(pos))
            return;

        done.add(pos);

        int old = iter.lookAhead();
        iter.move(pos);

        boolean next;
        do {
            pos = iter.next();
            next = scanOp(pos, iter, sub) && iter.hasNext();
        } while (next);

        iter.move(old);
    }

    private boolean scanOp(int pos, CodeIterator iter, Subroutine sub) throws BadBytecode {
        subroutines[pos] = sub;

        int opcode = iter.byteAt(pos);

        if (opcode == TABLESWITCH) {
            scanTableSwitch(pos, iter, sub);

            return false;
        }

        if (opcode == LOOKUPSWITCH) {
            scanLookupSwitch(pos, iter, sub);

            return false;
        }


        if (Util.isReturn(opcode) || opcode == RET || opcode == ATHROW)
            return false;

        if (Util.isJumpInstruction(opcode)) {
            int target = Util.getJumpTarget(pos, iter);
            if (opcode == JSR || opcode == JSR_W) {
                Subroutine s = subTable.get(target);
                if (s == null) {
                    s = new Subroutine(target, pos);
                    subTable.put(target, s);
                    scan(target, iter, s);
                } else {
                    s.addCaller(pos);
                }
            } else {
                scan(target, iter, sub);


                if (Util.isGoto(opcode))
                    return false;
            }
        }

        return true;
    }

    private void scanLookupSwitch(int pos, CodeIterator iter, Subroutine sub) throws BadBytecode {
        int index = (pos & ~3) + 4;

        scan(pos + iter.s32bitAt(index), iter, sub);
        int npairs = iter.s32bitAt(index += 4);
        int end = npairs * 8 + (index += 4);


        for (index += 4; index < end; index += 8) {
            int target = iter.s32bitAt(index) + pos;
            scan(target, iter, sub);
        }
    }

    private void scanTableSwitch(int pos, CodeIterator iter, Subroutine sub) throws BadBytecode {

        int index = (pos & ~3) + 4;

        scan(pos + iter.s32bitAt(index), iter, sub);
        int low = iter.s32bitAt(index += 4);
        int high = iter.s32bitAt(index += 4);
        int end = (high - low + 1) * 4 + (index += 4);


        for (; index < end; index += 4) {
            int target = iter.s32bitAt(index) + pos;
            scan(target, iter, sub);
        }
    }


}
