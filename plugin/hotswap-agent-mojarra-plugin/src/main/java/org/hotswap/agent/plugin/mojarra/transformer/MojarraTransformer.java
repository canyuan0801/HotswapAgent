
package org.hotswap.agent.plugin.mojarra.transformer;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtField;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.plugin.cdi.HaCdiCommons;


public class MojarraTransformer {

    @OnClassLoadEvent(classNameRegexp = "com.sun.faces.application.view.ViewScopeContext")
    public static void patchViewScopeContext(ClassPool classPool, CtClass ctClass) throws CannotCompileException, NotFoundException {
        HaCdiCommons.transformContext(classPool, ctClass);
    }

}
