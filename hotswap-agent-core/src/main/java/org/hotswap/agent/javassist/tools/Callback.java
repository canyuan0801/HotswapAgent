

package org.hotswap.agent.javassist.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.CtBehavior;


public abstract class Callback {

    public static Map<String,Callback> callbacks = new HashMap<String,Callback>();

    private final String sourceCode;


    public Callback(String src){
        String uuid = UUID.randomUUID().toString();
        callbacks.put(uuid, this);
        sourceCode = "((javassist.tools.Callback) javassist.tools.Callback.callbacks.get(\""+uuid+"\")).result(new Object[]{"+src+"});";
    }


    public abstract void result(Object[] objects);

    @Override
    public String toString(){
        return sourceCode();
    }

    public String sourceCode(){
        return sourceCode;
    }


    public static void insertBefore(CtBehavior behavior, Callback callback)
            throws CannotCompileException
    {
        behavior.insertBefore(callback.toString());
    }


    public static void insertAfter(CtBehavior behavior,Callback callback)
            throws CannotCompileException
    {
        behavior.insertAfter(callback.toString(), false);
    }


    public static void insertAfter(CtBehavior behavior, Callback callback, boolean asFinally)
            throws CannotCompileException
    {
        behavior.insertAfter(callback.toString(), asFinally);
    }


    public static int insertAt(CtBehavior behavior, Callback callback, int lineNum)
            throws CannotCompileException
    {
        return behavior.insertAt(lineNum, callback.toString());
    }
}
