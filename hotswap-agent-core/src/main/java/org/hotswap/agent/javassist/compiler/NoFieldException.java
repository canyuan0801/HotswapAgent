

package org.hotswap.agent.javassist.compiler;

import org.hotswap.agent.javassist.compiler.ast.ASTree;

public class NoFieldException extends CompileError {

    private static final long serialVersionUID = 1L;
    private String fieldName;
    private ASTree expr;


    public NoFieldException(String name, ASTree e) {
        super("no such field: " + name);
        fieldName = name;
        expr = e;
    }


    public String getField() { return fieldName; }


    public ASTree getExpr() { return expr; }
}
