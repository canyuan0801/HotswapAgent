

package org.hotswap.agent.javassist;


public interface Translator {

    void start(ClassPool pool)
        throws NotFoundException, CannotCompileException;


    void onLoad(ClassPool pool, String classname)
        throws NotFoundException, CannotCompileException;
}
