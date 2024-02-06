
package org.hotswap.agent.plugin.owb.transformer;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtField;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.cdi.HaCdiCommons;
import org.hotswap.agent.plugin.owb.beans.ContextualReloadHelper;
import org.hotswap.agent.plugin.owb.beans.OwbHotswapContext;


public class CdiContextsTransformer {

    private static AgentLogger LOGGER = AgentLogger.getLogger(CdiContextsTransformer.class);

    @OnClassLoadEvent(classNameRegexp = "(org.apache.webbeans.context.AbstractContext)|" +
                                        "(org.apache.myfaces.flow.cdi.FlowScopedContextImpl)|" +
                                        "(org.apache.myfaces.cdi.view.ViewScopeContextImpl)")
    public static void transformOwbContexts(CtClass clazz, ClassPool classPool, ClassLoader cl) throws NotFoundException, CannotCompileException {
        if (HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        CtClass superClass = clazz.getSuperclass();
        while (superClass != null) {
            if ("org.apache.webbeans.context.AbstractContext".equals(superClass.getName())) {
                return;
            }
            superClass = superClass.getSuperclass();
        }

        LOGGER.debug("Adding interface {} to {}.", OwbHotswapContext.class.getName(), clazz.getName());
        clazz.addInterface(classPool.get(OwbHotswapContext.class.getName()));

        CtField toReloadFld = CtField.make("public transient java.util.Set $$ha$toReloadOwb = null;", clazz);
        clazz.addField(toReloadFld);

        CtField reloadingFld = CtField.make("public transient boolean $$ha$reloadingOwb = false;", clazz);
        clazz.addField(reloadingFld);

        CtMethod addBeanToReload = CtMethod.make(
                "public void $$ha$addBeanToReloadOwb(javax.enterprise.context.spi.Contextual bean){" +
                "if ($$ha$toReloadOwb == null)" +
                    "$$ha$toReloadOwb = new java.util.HashSet();" +
                    "$$ha$toReloadOwb.add(bean);" +
                "}",
                clazz
        );
        clazz.addMethod(addBeanToReload);

        CtMethod getBeansToReload = CtMethod.make("public java.util.Set $$ha$getBeansToReloadOwb(){return $$ha$toReloadOwb;}", clazz);
        clazz.addMethod(getBeansToReload);

        CtMethod reload = CtMethod.make("public void $$ha$reloadOwb() {" + ContextualReloadHelper.class.getName() +".reload(this);}", clazz);
        clazz.addMethod(reload);

        CtMethod isActive = clazz.getDeclaredMethod("isActive");
        isActive.insertAfter(
                "if($_ && !$$ha$reloadingOwb ) { " +
                    "$$ha$reloadingOwb = true;" +
                    "$$ha$reloadOwb();" +
                    "$$ha$reloadingOwb = false;" +
                "}" +
                "return $_;"
        );



        LOGGER.debug("Class '{}' patched with hot-swapping support", clazz.getName() );
    }



    @OnClassLoadEvent(classNameRegexp = "org.apache.webbeans.context.AbstractContext")
    public static void transformAbstractContext(ClassPool classPool, CtClass ctClass) throws NotFoundException, CannotCompileException {
        if (HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        HaCdiCommons.transformContext(classPool, ctClass);
    }

}
