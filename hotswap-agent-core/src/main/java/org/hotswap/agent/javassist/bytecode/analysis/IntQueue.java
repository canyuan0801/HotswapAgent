
package org.hotswap.agent.javassist.bytecode.analysis;

import java.util.NoSuchElementException;

class IntQueue {
    private static class Entry {
        private IntQueue.Entry next;
        private int value;
        private Entry(int value) {
            this.value = value;
        }
    }
    private IntQueue.Entry head;

    private IntQueue.Entry tail;

    void add(int value) {
        IntQueue.Entry entry = new Entry(value);
        if (tail != null)
            tail.next = entry;
        tail = entry;

        if (head == null)
            head = entry;
    }

    boolean isEmpty() {
        return head == null;
    }

    int take() {
        if (head == null)
            throw new NoSuchElementException();

        int value = head.value;
        head = head.next;
        if (head == null)
            tail = null;

        return value;
    }
}
