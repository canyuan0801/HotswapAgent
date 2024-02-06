

package org.hotswap.agent.javassist.compiler;

import java.util.HashMap;

import org.hotswap.agent.javassist.compiler.ast.Declarator;

public final class SymbolTable extends HashMap<String,Declarator> {

    private static final long serialVersionUID = 1L;
    private SymbolTable parent;

    public SymbolTable() { this(null); }

    public SymbolTable(SymbolTable p) {
        super();
        parent = p;
    }

    public SymbolTable getParent() { return parent; }

    public Declarator lookup(String name) {
        Declarator found = get(name);
        if (found == null && parent != null)
            return parent.lookup(name);
        return found;
    }

    public void append(String name, Declarator value) {
        put(name, value);
    }
}
