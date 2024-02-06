

package org.hotswap.agent.javassist.runtime;


public class Cflow extends ThreadLocal<Cflow.Depth> {
    protected static class Depth {
        private int depth;
        Depth() { depth = 0; }
        int value() { return depth; }
        void inc() { ++depth; }
        void dec() { --depth; }
    }

    @Override
    protected synchronized Depth initialValue() {
        return new Depth();
    }


    public void enter() { get().inc(); }


    public void exit() { get().dec(); }


    public int value() { return get().value(); }
}
