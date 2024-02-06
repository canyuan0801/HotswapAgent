

package org.hotswap.agent.javassist.tools.reflect;

import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.NotFoundException;


public class Loader extends org.hotswap.agent.javassist.Loader {
    protected Reflection reflection;


    public static void main(String[] args) throws Throwable {
        Loader cl = new Loader();
        cl.run(args);
    }


    public Loader() throws CannotCompileException, NotFoundException {
        super();
        delegateLoadingOf("org.hotswap.agent.javassist.tools.reflect.Loader");

        reflection = new Reflection();
        ClassPool pool = ClassPool.getDefault();
        addTranslator(pool, reflection);
    }


    public boolean makeReflective(String clazz,
                                  String metaobject, String metaclass)
        throws CannotCompileException, NotFoundException
    {
        return reflection.makeReflective(clazz, metaobject, metaclass);
    }
}
