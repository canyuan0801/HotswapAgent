
package org.hotswap.agent.javassist.bytecode.analysis;



public class Frame {
    private Type[] locals;
    private Type[] stack;
    private int top;
    private boolean jsrMerged;
    private boolean retMerged;


    public Frame(int locals, int stack) {
        this.locals = new Type[locals];
        this.stack = new Type[stack];
    }


    public Type getLocal(int index) {
        return locals[index];
    }


    public void setLocal(int index, Type type) {
        locals[index] = type;
    }



    public Type getStack(int index) {
        return stack[index];
    }


    public void setStack(int index, Type type) {
        stack[index] = type;
    }


    public void clearStack() {
        top = 0;
    }


    public int getTopIndex() {
        return top - 1;
    }


    public int localsLength() {
        return locals.length;
    }


    public Type peek() {
        if (top < 1)
            throw new IndexOutOfBoundsException("Stack is empty");

        return stack[top - 1];
    }


    public Type pop() {
        if (top < 1)
            throw new IndexOutOfBoundsException("Stack is empty");
        return stack[--top];
    }


    public void push(Type type) {
        stack[top++] = type;
    }



    public Frame copy() {
        Frame frame = new Frame(locals.length, stack.length);
        System.arraycopy(locals, 0, frame.locals, 0, locals.length);
        System.arraycopy(stack, 0, frame.stack, 0, stack.length);
        frame.top = top;
        return frame;
    }


    public Frame copyStack() {
        Frame frame = new Frame(locals.length, stack.length);
        System.arraycopy(stack, 0, frame.stack, 0, stack.length);
        frame.top = top;
        return frame;
    }


    public boolean mergeStack(Frame frame) {
        boolean changed = false;
        if (top != frame.top)
            throw new RuntimeException("Operand stacks could not be merged, they are different sizes!");

        for (int i = 0; i < top; i++) {
            if (stack[i] != null) {
                Type prev = stack[i];
                Type merged = prev.merge(frame.stack[i]);
                if (merged == Type.BOGUS)
                    throw new RuntimeException("Operand stacks could not be merged due to differing primitive types: pos = " + i);

                stack[i] = merged;

                if ((! merged.equals(prev)) || merged.popChanged()) {
                    changed = true;
                }
            }
        }

        return changed;
    }


    public boolean merge(Frame frame) {
        boolean changed = false;


        for (int i = 0; i < locals.length; i++) {
            if (locals[i] != null) {
                Type prev = locals[i];
                Type merged = prev.merge(frame.locals[i]);

                locals[i] = merged;
                if (! merged.equals(prev) || merged.popChanged()) {
                    changed = true;
                }
            } else if (frame.locals[i] != null) {
                locals[i] = frame.locals[i];
                changed = true;
            }
        }

        changed |= mergeStack(frame);
        return changed;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("locals = [");
        for (int i = 0; i < locals.length; i++) {
            buffer.append(locals[i] == null ? "empty" : locals[i].toString());
            if (i < locals.length - 1)
                buffer.append(", ");
        }
        buffer.append("] stack = [");
        for (int i = 0; i < top; i++) {
            buffer.append(stack[i]);
            if (i < top - 1)
                buffer.append(", ");
        }
        buffer.append("]");

        return buffer.toString();
    }


    boolean isJsrMerged() {
        return jsrMerged;
    }


    void setJsrMerged(boolean jsrMerged) {
        this.jsrMerged = jsrMerged;
    }


    boolean isRetMerged() {
        return retMerged;
    }


    void setRetMerged(boolean retMerged) {
        this.retMerged = retMerged;
    }
}
