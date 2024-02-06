
package org.hotswap.agent.plugin.weld_jakarta.transformer;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.cdi.HaCdiCommons;

public class AbstractClassBeanTransformer {

    private static AgentLogger LOGGER = AgentLogger.getLogger(AbstractClassBeanTransformer.class);


    @OnClassLoadEvent(classNameRegexp = "org.jboss.weld.bean.AbstractClassBean")
    public static void transformAbstractClassBean(CtClass ctClass, ClassPool classPool) throws NotFoundException, CannotCompileException {
        if (!HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        CtMethod method = ctClass.getDeclaredMethod("cleanupAfterBoot");
        method.setBody("{ }");
        LOGGER.debug("AbstractClassBean.cleanupAfterBoot patched");
    }
}
