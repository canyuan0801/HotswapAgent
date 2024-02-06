
package org.hotswap.agent.javassist.bytecode.analysis;

import java.io.PrintStream;

import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.Modifier;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.bytecode.BadBytecode;
import org.hotswap.agent.javassist.bytecode.CodeAttribute;
import org.hotswap.agent.javassist.bytecode.CodeIterator;
import org.hotswap.agent.javassist.bytecode.ConstPool;
import org.hotswap.agent.javassist.bytecode.Descriptor;
import org.hotswap.agent.javassist.bytecode.InstructionPrinter;
import org.hotswap.agent.javassist.bytecode.MethodInfo;


public final class FramePrinter {
    private final PrintStream stream;


    public FramePrinter(PrintStream stream) {
        this.stream = stream;
    }


    public static void print(CtClass clazz, PrintStream stream) {
        (new FramePrinter(stream)).print(clazz);
    }


    public void print(CtClass clazz) {
        CtMethod[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            print(methods[i]);
        }
    }

    private String getMethodString(CtMethod method) {
        try {
            return Modifier.toString(method.getModifiers()) + " "
                    + method.getReturnType().getName() + " " + method.getName()
                    + Descriptor.toString(method.getSignature()) + ";";
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public void print(CtMethod method) {
        stream.println("\n" + getMethodString(method));
        MethodInfo info = method.getMethodInfo2();
        ConstPool pool = info.getConstPool();
        CodeAttribute code = info.getCodeAttribute();
        if (code == null)
            return;

        Frame[] frames;
        try {
            frames = (new Analyzer()).analyze(method.getDeclaringClass(), info);
        } catch (BadBytecode e) {
            throw new RuntimeException(e);
        }

        int spacing = String.valueOf(code.getCodeLength()).length();

        CodeIterator iterator = code.iterator();
        while (iterator.hasNext()) {
            int pos;
            try {
                pos = iterator.next();
            } catch (BadBytecode e) {
                throw new RuntimeException(e);
            }

            stream.println(pos + ": " + InstructionPrinter.instructionString(iterator, pos, pool));

            addSpacing(spacing + 3);
            Frame frame = frames[pos];
            if (frame == null) {
                stream.println("--DEAD CODE--");
                continue;
            }
            printStack(frame);

            addSpacing(spacing + 3);
            printLocals(frame);
        }

    }

    private void printStack(Frame frame) {
        stream.print("stack [");
        int top = frame.getTopIndex();
        for (int i = 0; i <= top; i++) {
            if (i > 0)
                stream.print(", ");
            Type type = frame.getStack(i);
            stream.print(type);
        }
        stream.println("]");
    }

    private void printLocals(Frame frame) {
        stream.print("locals [");
        int length = frame.localsLength();
        for (int i = 0; i < length; i++) {
            if (i > 0)
                stream.print(", ");
            Type type = frame.getLocal(i);
            stream.print(type == null ? "empty" : type.toString());
        }
        stream.println("]");
    }

    private void addSpacing(int count) {
        while (count-- > 0)
            stream.print(' ');
    }
}
