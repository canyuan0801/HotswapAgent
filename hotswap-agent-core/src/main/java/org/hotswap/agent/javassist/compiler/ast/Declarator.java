

package org.hotswap.agent.javassist.compiler.ast;

import org.hotswap.agent.javassist.compiler.CompileError;
import org.hotswap.agent.javassist.compiler.TokenId;


public class Declarator extends ASTList implements TokenId {

    private static final long serialVersionUID = 1L;
    protected int varType;
    protected int arrayDim;
    protected int localVar;
    protected String qualifiedClass;

    public Declarator(int type, int dim) {
        super(null);
        varType = type;
        arrayDim = dim;
        localVar = -1;
        qualifiedClass = null;
    }

    public Declarator(ASTList className, int dim) {
        super(null);
        varType = CLASS;
        arrayDim = dim;
        localVar = -1;
        qualifiedClass = astToClassName(className, '/');
    }


    public Declarator(int type, String jvmClassName, int dim,
                      int var, Symbol sym) {
        super(null);
        varType = type;
        arrayDim = dim;
        localVar = var;
        qualifiedClass = jvmClassName;
        setLeft(sym);
        append(this, null);
    }

    public Declarator make(Symbol sym, int dim, ASTree init) {
        Declarator d = new Declarator(this.varType, this.arrayDim + dim);
        d.qualifiedClass = this.qualifiedClass;
        d.setLeft(sym);
        append(d, init);
        return d;
    }


    public int getType() { return varType; }

    public int getArrayDim() { return arrayDim; }

    public void addArrayDim(int d) { arrayDim += d; }

    public String getClassName() { return qualifiedClass; }

    public void setClassName(String s) { qualifiedClass = s; }

    public Symbol getVariable() { return (Symbol)getLeft(); }

    public void setVariable(Symbol sym) { setLeft(sym); }

    public ASTree getInitializer() {
        ASTList t = tail();
        if (t != null)
            return t.head();
        return null;
    }

    public void setLocalVar(int n) { localVar = n; }

    public int getLocalVar() { return localVar; }

    @Override
    public String getTag() { return "decl"; }

    @Override
    public void accept(Visitor v) throws CompileError {
        v.atDeclarator(this);
    }

    public static String astToClassName(ASTList name, char sep) {
        if (name == null)
            return null;

        StringBuffer sbuf = new StringBuffer();
        astToClassName(sbuf, name, sep);
        return sbuf.toString();
    }

    private static void astToClassName(StringBuffer sbuf, ASTList name,
                                       char sep) {
        for (;;) {
            ASTree h = name.head();
            if (h instanceof Symbol)
                sbuf.append(((Symbol)h).get());
            else if (h instanceof ASTList)
                astToClassName(sbuf, (ASTList)h, sep);

            name = name.tail();
            if (name == null)
                break;

            sbuf.append(sep);
        }
    }
}
