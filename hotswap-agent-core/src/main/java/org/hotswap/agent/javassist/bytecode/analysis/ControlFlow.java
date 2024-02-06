

package org.hotswap.agent.javassist.bytecode.analysis;

import java.util.ArrayList;
import java.util.List;

import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.MethodInfo;
import org.hotswap.agent.javassist.bytecode.stackmap.BasicBlock;


public class ControlFlow {
    private CtClass clazz;
    private MethodInfo methodInfo;
    private Block[] basicBlocks;
    private Frame[] frames;


    public ControlFlow(CtMethod method) throws BadBytecode {
        this(method.getDeclaringClass(), method.getMethodInfo2());
    }


    public ControlFlow(CtClass ctclazz, MethodInfo minfo) throws BadBytecode {
        clazz = ctclazz;
        methodInfo = minfo;
        frames = null;
        basicBlocks = (Block[])new BasicBlock.Maker() {
            @Override
            protected BasicBlock makeBlock(int pos) {
                return new Block(pos, methodInfo);
            }
            @Override
            protected BasicBlock[] makeArray(int size) {
                return new Block[size];
            }
        }.make(minfo);
        if (basicBlocks == null)
            basicBlocks = new Block[0];
        int size = basicBlocks.length;
        int[] counters = new int[size];
        for (int i = 0; i < size; i++) {
            Block b = basicBlocks[i];
            b.index = i;
            b.entrances = new Block[b.incomings()];
            counters[i] = 0;
        }

        for (int i = 0; i < size; i++) {
            Block b = basicBlocks[i];
            for (int k = 0; k < b.exits(); k++) {
                Block e = b.exit(k);
                e.entrances[counters[e.index]++] = b;
            }

            ControlFlow.Catcher[] catchers = b.catchers();
            for (int k = 0; k < catchers.length; k++) {
                Block catchBlock = catchers[k].node;
                catchBlock.entrances[counters[catchBlock.index]++] = b;
            }
        }
    }


    public Block[] basicBlocks() {
        return basicBlocks;
    }


    public Frame frameAt(int pos) throws BadBytecode {
        if (frames == null)
            frames = new Analyzer().analyze(clazz, methodInfo);

        return frames[pos];
    }


    public Node[] dominatorTree() {
        int size = basicBlocks.length;
        if (size == 0)
            return null;

        Node[] nodes = new Node[size];
        boolean[] visited = new boolean[size];
        int[] distance = new int[size];
        for (int i = 0; i < size; i++) {
            nodes[i] = new Node(basicBlocks[i]);
            visited[i] = false;
        }

        Access access = new Access(nodes) {
            @Override
            BasicBlock[] exits(Node n) { return n.block.getExit(); }
            @Override
            BasicBlock[] entrances(Node n) { return n.block.entrances; }
        };
        nodes[0].makeDepth1stTree(null, visited, 0, distance, access);
        do {
            for (int i = 0; i < size; i++)
                visited[i] = false;
        } while (nodes[0].makeDominatorTree(visited, distance, access));
        Node.setChildren(nodes);
        return nodes;
    }


    public Node[] postDominatorTree() {
        int size = basicBlocks.length;
        if (size == 0)
            return null;

        Node[] nodes = new Node[size];
        boolean[] visited = new boolean[size];
        int[] distance = new int[size];
        for (int i = 0; i < size; i++) {
            nodes[i] = new Node(basicBlocks[i]);
            visited[i] = false;
        }

        Access access = new Access(nodes) {
            @Override
            BasicBlock[] exits(Node n) { return n.block.entrances; }
            @Override
            BasicBlock[] entrances(Node n) { return n.block.getExit(); }
        };

        int counter = 0;
        for (int i = 0; i < size; i++)
            if (nodes[i].block.exits() == 0)
                counter = nodes[i].makeDepth1stTree(null, visited, counter, distance, access);

        boolean changed;
        do {
            for (int i = 0; i < size; i++)
                visited[i] = false;

            changed = false;
            for (int i = 0; i < size; i++)
                if (nodes[i].block.exits() == 0)
                    if (nodes[i].makeDominatorTree(visited, distance, access))
                        changed = true;
        } while (changed);

        Node.setChildren(nodes);
        return nodes;
    }


    public static class Block extends BasicBlock {

        public Object clientData = null;

        int index;
        MethodInfo method;
        Block[] entrances;

        Block(int pos, MethodInfo minfo) {
            super(pos);
            method = minfo;
        }

        @Override
        protected void toString2(StringBuffer sbuf) {
            super.toString2(sbuf);
            sbuf.append(", incoming{");
            for (int i = 0; i < entrances.length; i++)
                    sbuf.append(entrances[i].position).append(", ");

            sbuf.append("}");
        }

        BasicBlock[] getExit() { return exit; }


        public int index() { return index; }


        public int position() { return position; }


        public int length() { return length; }


        public int incomings() { return incoming; }


        public Block incoming(int n) {
            return entrances[n];
        }


        public int exits() { return exit == null ? 0 : exit.length; }


        public Block exit(int n) { return (Block)exit[n]; }


        public Catcher[] catchers() {
            List<Catcher> catchers = new ArrayList<Catcher>();
            BasicBlock.Catch c = toCatch;
            while (c != null) {
                catchers.add(new Catcher(c));
                c = c.next;
            }

            return catchers.toArray(new Catcher[catchers.size()]);
        }
    }

    static abstract class Access {
        Node[] all;
        Access(Node[] nodes) { all = nodes; }
        Node node(BasicBlock b) { return all[((Block)b).index]; } 
        abstract BasicBlock[] exits(Node n);
        abstract BasicBlock[] entrances(Node n);
    }


    public static class Node {
        private Block block;
        private Node parent;
        private Node[] children;

        Node(Block b) {
            block = b;
            parent = null;
        }


        @Override
        public String toString() {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append("Node[pos=").append(block().position());
            sbuf.append(", parent=");
            sbuf.append(parent == null ? "*" : Integer.toString(parent.block().position()));
            sbuf.append(", children{");
            for (int i = 0; i < children.length; i++)
                sbuf.append(children[i].block().position()).append(", ");

            sbuf.append("}]");
            return sbuf.toString();
        }


        public Block block() { return block; }


        public Node parent() { return parent; }


        public int children() { return children.length; }


        public Node child(int n) { return children[n]; }


        int makeDepth1stTree(Node caller, boolean[] visited, int counter, int[] distance, Access access) {
            int index = block.index;
            if (visited[index])
                return counter;

            visited[index] = true;
            parent = caller;
            BasicBlock[] exits = access.exits(this);
            if (exits != null)
                for (int i = 0; i < exits.length; i++) {
                    Node n = access.node(exits[i]);
                    counter = n.makeDepth1stTree(this, visited, counter, distance, access);
                }

            distance[index] = counter++;
            return counter;
        }

        boolean makeDominatorTree(boolean[] visited, int[] distance, Access access) {
            int index = block.index;
            if (visited[index])
                return false;

            visited[index] = true;
            boolean changed = false;
            BasicBlock[] exits = access.exits(this);
            if (exits != null)
                for (int i = 0; i < exits.length; i++) {
                    Node n = access.node(exits[i]);
                    if (n.makeDominatorTree(visited, distance, access))
                        changed = true;
                }

            BasicBlock[] entrances = access.entrances(this);
            if (entrances != null)
                for (int i = 0; i < entrances.length; i++) {
                    if (parent != null) {
                        Node n = getAncestor(parent, access.node(entrances[i]), distance);
                        if (n != parent) {
                            parent = n;
                            changed = true;
                        }
                    }
                }

            return changed;
        }

        private static Node getAncestor(Node n1, Node n2, int[] distance) {
            while (n1 != n2) {
                if (distance[n1.block.index] < distance[n2.block.index])
                    n1 = n1.parent;
                else
                    n2 = n2.parent;

                if (n1 == null || n2 == null)
                    return null;
            }

            return n1;
        }

        private static void setChildren(Node[] all) {
            int size = all.length;
            int[] nchildren = new int[size];
            for (int i = 0; i < size; i++)
                nchildren[i] = 0;

            for (int i = 0; i < size; i++) {
                Node p = all[i].parent;
                if (p != null)
                    nchildren[p.block.index]++;
            }

            for (int i = 0; i < size; i++)
                all[i].children = new Node[nchildren[i]];

            for (int i = 0; i < size; i++)
                nchildren[i] = 0;

            for (int i = 0; i < size; i++) {
                Node n = all[i];
                Node p = n.parent;
                if (p != null)
                    p.children[nchildren[p.block.index]++] = n;
            }
        }
    }


    public static class Catcher {
        private Block node;
        private int typeIndex;

        Catcher(BasicBlock.Catch c) {
            node = (Block)c.body;
            typeIndex = c.typeIndex;
        }


        public Block block() { return node; }


        public String type() {
            if (typeIndex == 0)
                return "java.lang.Throwable";
            else
                return node.method.getConstPool().getClassInfo(typeIndex);
        }
    }
}
