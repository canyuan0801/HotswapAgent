

package org.hotswap.agent.javassist.compiler;

import java.util.HashMap;

public final class KeywordTable extends HashMap<String,Integer> {

    private static final long serialVersionUID = 1L;

    public KeywordTable() { super(); }

    public int lookup(String name) {
        return containsKey(name) ? get(name) : -1;
    }

    public void append(String name, int t) {
        put(name, t);
    }
}
