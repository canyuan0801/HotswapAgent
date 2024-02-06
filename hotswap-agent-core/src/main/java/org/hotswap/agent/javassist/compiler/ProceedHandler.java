

package org.hotswap.agent.javassist.compiler;

import org.hotswap.agent.javassist.bytecode.Bytecode;
import org.hotswap.agent.javassist.compiler.ast.ASTList;


public interface ProceedHandler {
    void doit(JvstCodeGen gen, Bytecode b, ASTList args) throws CompileError;
    void setReturnType(JvstTypeChecker c, ASTList args) throws CompileError;
}
