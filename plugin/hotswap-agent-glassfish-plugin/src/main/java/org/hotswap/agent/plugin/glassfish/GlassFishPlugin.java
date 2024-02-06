
package org.hotswap.agent.plugin.glassfish;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;


@Plugin(name = "GlassFish",
        description = "GlassFish - glassfish server.",
        testedVersions = {""},
        expectedVersions = {""},
        supportClass={WebappClassLoaderTransformer.class}
)
public class GlassFishPlugin {

    protected static AgentLogger LOGGER = AgentLogger.getLogger(GlassFishPlugin.class);

    private static String FRAMEWORK_BOOTDELEGATION = "org.osgi.framework.bootdelegation";

    private static final String BOOTDELEGATION_PACKAGES =
            "org.hotswap.agent, " +
            "org.hotswap.agent.*";


    @OnClassLoadEvent(classNameRegexp = "org.apache.felix.framework.Felix")
    public static void transformFelix(ClassPool classPool, CtClass ctClass) throws NotFoundException, CannotCompileException {

        CtClass[] constructorParams = new CtClass[] {
            classPool.get("java.util.Map")
        };

        CtConstructor declaredConstructor = ctClass.getDeclaredConstructor(constructorParams);
        declaredConstructor.insertBefore(
                "{" +
                    "if ($1 == null) { " +
                        "$1 = new java.util.HashMap();" +
                    "}" +
                    "String $$ha$bootDeleg = (String) $1.get(\"" + FRAMEWORK_BOOTDELEGATION + "\");" +
                    "if ($$ha$bootDeleg == null) {" +
                        "$$ha$bootDeleg = \"\";" +
                    "}" +
                    "if ($$ha$bootDeleg.indexOf(\"org.hotswap.agent\") == -1) {" +
                        "$$ha$bootDeleg = $$ha$bootDeleg.trim();" +
                        "if (!$$ha$bootDeleg.isEmpty()) {" +
                            "$$ha$bootDeleg = $$ha$bootDeleg + \", \";" +
                        "}" +
                        "$$ha$bootDeleg = $$ha$bootDeleg + \"" + BOOTDELEGATION_PACKAGES + "\";" +
                        "$1.put(\"" + FRAMEWORK_BOOTDELEGATION + "\", $$ha$bootDeleg);" +
                    "}" +
                "}"
        );

        LOGGER.debug("Class 'org.apache.felix.framework.Felix' patched in classLoader {}.");
    }

    @OnClassLoadEvent(classNameRegexp = "org.apache.felix.framework.BundleWiringImpl")
    public static void transformBundleClassLoader(ClassPool classPool, CtClass ctClass) throws NotFoundException, CannotCompileException {








    }
}
