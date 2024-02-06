
package org.hotswap.agent.plugin.deltaspike.transformer;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.CtNewMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.cdi.HaCdiCommons;
import org.hotswap.agent.plugin.deltaspike.DeltaSpikePlugin;
import org.hotswap.agent.util.PluginManagerInvoker;


public class RepositoryTransformer {

    private static AgentLogger LOGGER = AgentLogger.getLogger(RepositoryTransformer.class);

    public static final String REINITIALIZE_METHOD = "$$ha$reinitialize";

    
    @OnClassLoadEvent(classNameRegexp = "org.apache.deltaspike.data.impl.meta.RepositoryComponent")
    public static void patchRepositoryComponent(ClassPool classPool, CtClass ctClass) throws CannotCompileException {
        if (HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        StringBuilder src = new StringBuilder("{");
        src.append(PluginManagerInvoker.buildInitializePlugin(DeltaSpikePlugin.class));
        src.append(PluginManagerInvoker.buildCallPluginMethod(DeltaSpikePlugin.class, "registerRepoComponent",
                "this", "java.lang.Object",
                "this.repoClass", "java.lang.Class"));
        src.append("}");

        for (CtConstructor constructor : ctClass.getDeclaredConstructors()) {
            constructor.insertAfter(src.toString());
        }

        ctClass.addMethod(CtNewMethod.make("public void " + REINITIALIZE_METHOD + "() {" +
                "   this.methods.clear(); " +
                "   initialize();" +
                "}", ctClass));

        LOGGER.debug("org.apache.deltaspike.data.impl.meta.RepositoryComponent - registration hook and reinitialization method added.");
    }


    
    @OnClassLoadEvent(classNameRegexp = "org.apache.deltaspike.data.impl.meta.RepositoryMetadataHandler")
    public static void patchRepositoryMetadataHandler(ClassPool classPool, CtClass ctClass) throws CannotCompileException, NotFoundException {
        if (HaCdiCommons.isJakarta(classPool)) {
            return;
        }
        ctClass.addMethod(CtNewMethod.make("public void " + REINITIALIZE_METHOD + "(java.lang.Class repositoryClass) {" +
                    "org.apache.deltaspike.data.impl.meta.RepositoryMetadata metadata = metadataInitializer.init(repositoryClass, this.beanManager);" +
                    "this.repositoriesMetadata.put(repositoryClass, metadata);" +
                "}", ctClass));

        LOGGER.debug("org.apache.deltaspike.data.impl.meta.RepositoryMetadataHandler - registration hook and reinitialization method added.");
    }

    
    @OnClassLoadEvent(classNameRegexp = "org.apache.deltaspike.data.impl.RepositoryExtension")
    public static void patchRepositoryExtension(ClassPool classPool, CtClass ctClass) throws CannotCompileException, NotFoundException, org.hotswap.agent.javassist.CannotCompileException {
        if (HaCdiCommons.isJakarta(classPool)) {
            return;
        }

        if (ctClass.getSuperclassName().equals(Object.class.getName())) {
            ctClass.setSuperclass(classPool.get(HaAfteBeanDiscovery.class.getName()));
        } else {
            LOGGER.error("org.apache.deltaspike.data.impl.RepositoryExtension patch failed. Expected superclass java.lang.Object, found:" + ctClass.getSuperclassName());
        }

        LOGGER.debug("org.apache.deltaspike.data.impl.RepositoryExtension - registration hook and registration repository classes added.");
    }
}
