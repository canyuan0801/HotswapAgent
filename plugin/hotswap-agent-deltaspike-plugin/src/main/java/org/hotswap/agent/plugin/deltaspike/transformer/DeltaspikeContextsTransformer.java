
package org.hotswap.agent.plugin.deltaspike.transformer;


import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.plugin.cdi.HaCdiCommons;


public class DeltaspikeContextsTransformer {

    @OnClassLoadEvent(classNameRegexp = "org.apache.deltaspike.core.util.context.AbstractContext")
    public static void patchWindowContext(ClassPool classPool, CtClass ctClass) throws CannotCompileException, NotFoundException {
        if (HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        HaCdiCommons.transformContext(classPool, ctClass);
    }
}
