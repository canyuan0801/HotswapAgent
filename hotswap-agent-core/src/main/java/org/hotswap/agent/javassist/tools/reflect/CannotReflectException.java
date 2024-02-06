

package org.hotswap.agent.javassist.tools.reflect;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.CtClass;


public class CannotReflectException extends CannotCompileException {

    private static final long serialVersionUID = 1L;

    public CannotReflectException(String msg) {
        super(msg);
    }
}
