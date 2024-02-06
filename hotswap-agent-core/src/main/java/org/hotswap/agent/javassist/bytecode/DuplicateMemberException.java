

package org.hotswap.agent.javassist.bytecode;

import org.hotswap.agent.javassist.CannotCompileException;


public class DuplicateMemberException extends CannotCompileException {

    private static final long serialVersionUID = 1L;

    public DuplicateMemberException(String msg) {
        super(msg);
    }
}
