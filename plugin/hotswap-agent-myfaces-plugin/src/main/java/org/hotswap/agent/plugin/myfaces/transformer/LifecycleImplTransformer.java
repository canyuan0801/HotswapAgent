
package org.hotswap.agent.plugin.myfaces.transformer;

import static org.hotswap.agent.plugin.myfaces.MyFacesConstants.LIFECYCLE_IMPL_CLASS;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;


public class LifecycleImplTransformer {

    private static AgentLogger LOGGER = AgentLogger.getLogger(LifecycleImplTransformer.class);

    private static Boolean isJavax;

    private static boolean isJavax(ClassPool classPool) {
        if (isJavax == null) {
            try {
                classPool.get("javax.faces.context.FacesContext");
                isJavax = true;
            } catch (NotFoundException e) {
                isJavax = false;
            }
        }
        return isJavax;
    }


    @OnClassLoadEvent(classNameRegexp = LIFECYCLE_IMPL_CLASS)
    public static void init(ClassLoader classLoader, ClassPool classPool, CtClass ctClass) throws CannotCompileException, NotFoundException {
        if (!isJavax(classPool)) {
            return;
        }
        LOGGER.info("Patching lifecycle implementation. classLoader: {}", classLoader);

        initClassPool(ctClass);

        patchExecuteMethod(ctClass);

        LOGGER.info("Patched lifecycle implementation successfully.");
    }

    private static void initClassPool(CtClass ctClass) throws CannotCompileException, NotFoundException {
        ClassPool classPool = ctClass.getClassPool();

        CtClass modifiedResolverCtClass = ManagedBeanResolverTransformer.getModifiedCtClass(classPool);

        modifiedResolverCtClass.defrost();
        classPool.makeClass(modifiedResolverCtClass.getClassFile());

        classPool.importPackage("javax.faces.context");
        classPool.importPackage("java.util");
        classPool.importPackage("org.apache.myfaces.el.unified.resolver");
    }


    private static void patchExecuteMethod(CtClass ctClass) throws CannotCompileException, NotFoundException {
        ClassPool classPool = ctClass.getClassPool();

        CtMethod executeMethod = ctClass.getDeclaredMethod("execute", new CtClass[] {
            classPool.get("javax.faces.context.FacesContext"),
        });

        String processDirtyBeanCall = 
            "ManagedBeanResolver beanResolver = new ManagedBeanResolver(); " +
            "beanResolver.processDirtyBeans(); "
            ;
        
        executeMethod.insertAfter(processDirtyBeanCall);
    }

}
