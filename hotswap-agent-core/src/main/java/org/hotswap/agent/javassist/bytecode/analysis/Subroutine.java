
package org.hotswap.agent.javassist.bytecode.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Subroutine {

    private List<Integer> callers = new ArrayList<Integer>();
    private Set<Integer> access = new HashSet<Integer>();
    private int start;

    public Subroutine(int start, int caller) {
        this.start = start;
        callers.add(caller);
    }

    public void addCaller(int caller) {
        callers.add(caller);
    }

    public int start() {
        return start;
    }

    public void access(int index) {
        access.add(index);
    }

    public boolean isAccessed(int index) {
        return access.contains(index);
    }

    public Collection<Integer> accessed() {
        return access;
    }

    public Collection<Integer> callers() {
        return callers;
    }

    @Override
    public String toString() {
        return "start = " + start + " callers = " + callers.toString();
    }
}
