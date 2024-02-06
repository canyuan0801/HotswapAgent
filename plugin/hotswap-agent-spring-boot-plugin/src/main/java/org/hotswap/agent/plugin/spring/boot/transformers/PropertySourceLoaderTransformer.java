
package org.hotswap.agent.plugin.spring.boot.transformers;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.javassist.expr.ConstructorCall;
import org.hotswap.agent.javassist.expr.ExprEditor;
import org.hotswap.agent.javassist.expr.MethodCall;
import org.hotswap.agent.javassist.expr.NewExpr;
import org.hotswap.agent.logging.AgentLogger;

import java.security.ProtectionDomain;

public class PropertySourceLoaderTransformer {

    private static AgentLogger LOGGER = AgentLogger.getLogger(PropertySourceLoaderTransformer.class);

    @OnClassLoadEvent(classNameRegexp = "org.springframework.boot.env.YamlPropertySourceLoader")
    public static void transformYamlPropertySourceLoader(CtClass clazz, ClassPool classPool) throws
        NotFoundException, CannotCompileException {
        enhanceBasePropertySourceLoader(clazz);

        CtMethod ctMethod = clazz.getDeclaredMethod("load");
        if (ctMethod.getParameterTypes().length == 2) {
            LOGGER.debug("Patch org.springframework.boot.env.YamlPropertySourceLoader with 2 parameters");
            ctMethod.addLocalVariable(reloadVariableName(),
                classPool.get("org.hotswap.agent.plugin.spring.boot.env.v2.YamlPropertySourceLoader"));
            ctMethod.insertBefore(
                "{" + reloadVariableName() + " = new org.hotswap.agent.plugin.spring.boot.env.v2.YamlPropertySourceLoader($1, $2);}");
            ctMethod.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getClassName().equals("org.springframework.boot.env.OriginTrackedYamlLoader")
                        && m.getMethodName().equals("load")) {
                        m.replace("{$_ = " + reloadVariableName() + ".load();}");
                    } else {
                        makeMapWritable(m);
                    }
                }
                @Override
                public void edit(NewExpr e) {
                    makePropertySourceWritable(e);
                }
            });
            ctMethod.insertAfter("{ $$ha$loadList0($_ , " + reloadVariableName() + ");}");
        } else if (ctMethod.getParameterTypes().length == 3) {
            LOGGER.debug("Patch org.springframework.boot.env.YamlPropertySourceLoader with 3 parameters");
            ctMethod.addLocalVariable(reloadVariableName(),
                classPool.get("org.hotswap.agent.plugin.spring.boot.env.v1.YamlPropertySourceLoader"));
            ctMethod.insertBefore(
                "{" + reloadVariableName() + " = new org.hotswap.agent.plugin.spring.boot.env.v1.YamlPropertySourceLoader($1, $2, $3);}");
            ctMethod.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getClassName().equals("org.springframework.boot.env.YamlPropertySourceLoader$Processor")
                        && m.getMethodName().equals("process")) {
                        m.replace("{$_ = " + reloadVariableName() + ".load();}");
                    } else {
                        makeMapWritable(m);
                    }
                }
                @Override
                public void edit(NewExpr e) {
                    makePropertySourceWritable(e);
                }
            });
            ctMethod.insertAfter("{ $$ha$load0($_ , " + reloadVariableName() + "); }");
        }

        LOGGER.debug("Patch org.springframework.boot.env.YamlPropertySourceLoader success");
    }

    @OnClassLoadEvent(classNameRegexp = "org.springframework.boot.env.PropertiesPropertySourceLoader")
    public static void transformPropertiesPropertySourceLoader(CtClass clazz, ClassPool classPool)
        throws NotFoundException, CannotCompileException {
        enhanceBasePropertySourceLoader(clazz);

        CtMethod ctMethod = clazz.getDeclaredMethod("load");
        if (ctMethod.getParameterTypes().length == 2) {
            if (isSpringBoot2LowerVersion(clazz, classPool)) {
                LOGGER.debug(
                    "Patch org.springframework.boot.env.PropertiesPropertySourceLoader with 2 parameters and lower "
                        + "version");
                ctMethod.addLocalVariable(reloadVariableName(), classPool.get(
                    "org.hotswap.agent.plugin.spring.boot.env.v2.LowVersionPropertiesPropertySourceLoader"));
                ctMethod.insertBefore(
                    "{" + reloadVariableName() + " = new org.hotswap.agent.plugin.spring.boot.env.v2"
                        + ".LowVersionPropertiesPropertySourceLoader($0, $1, $2);}");
            } else {
                LOGGER.debug(
                    "Patch org.springframework.boot.env.PropertiesPropertySourceLoader with 2 parameters and not "
                        + "lower version");
                ctMethod.addLocalVariable(reloadVariableName(),
                    classPool.get("org.hotswap.agent.plugin.spring.boot.env.v2.PropertiesPropertySourceLoader"));
                ctMethod.insertBefore(
                    "{" + reloadVariableName() + " = new org.hotswap.agent.plugin.spring.boot.env.v2.PropertiesPropertySourceLoader($0, "
                        + "$1, $2);}");
            }
            ctMethod.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("loadProperties")) {
                        m.replace("{$_ = " + reloadVariableName() + ".load();}");
                    } else {
                        makeMapWritable(m);
                    }
                }

                @Override
                public void edit(NewExpr e) {
                    makePropertySourceWritable(e);
                }
            });
            ctMethod.insertAfter("{ $$ha$loadList0($_ , " + reloadVariableName() + "); }");
        } else if (ctMethod.getParameterTypes().length == 3) {
            LOGGER.debug("Patch org.springframework.boot.env.PropertiesPropertySourceLoader with 3 parameters");
            ctMethod.addLocalVariable(reloadVariableName(),
                classPool.get("org.hotswap.agent.plugin.spring.boot.env.v1.PropertiesPropertySourceLoader"));
            ctMethod.insertBefore(
                "{" + reloadVariableName() + " = new org.hotswap.agent.plugin.spring.boot.env.v1.PropertiesPropertySourceLoader($1, $2, "
                    + "$3);}");
            ctMethod.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getClassName().equals("org.springframework.core.io.support.PropertiesLoaderUtils")
                        && m.getMethodName().equals("loadProperties")) {
                        m.replace("{$_ = (java.util.Properties)" + reloadVariableName() + ".load();}");
                    } else {
                        makeMapWritable(m);
                    }
                }
                @Override
                public void edit(NewExpr e) {
                    makePropertySourceWritable(e);
                }
            });
            ctMethod.insertAfter("{ $$ha$load0($_ , " + reloadVariableName() + "); }");
        }

        LOGGER.debug("Patch org.springframework.boot.env.YamlPropertySourceLoader success");
    }

    private static String reloadVariableName() {
        return "$$ha$reload";
    }

    private static boolean isSpringBoot2LowerVersion(CtClass clazz, ClassPool classPool) {
        try {
            CtMethod ctMethod = clazz.getDeclaredMethod("loadProperties",
                new CtClass[] {classPool.get("org.springframework.core.io.Resource")});
            if ("java.util.Map".equals(ctMethod.getReturnType().getName())) {
                return true;
            } else {
                return false;
            }
        } catch (Exception t) {
            return true;
        }
    }

    private static void enhanceBasePropertySourceLoader(CtClass clazz) throws CannotCompileException {
        clazz.addMethod(CtMethod.make("private void $$ha$load0(org.springframework.core.env.PropertySource p, " +
            "org.hotswap.agent.plugin.spring.api.PropertySourceReloader r) throws java.io.IOException { " +
            "if (p instanceof org.hotswap.agent.plugin.spring.transformers.api.ReloadablePropertySource) { " +
            "((org.hotswap.agent.plugin.spring.transformers.api.ReloadablePropertySource) p).setReload(r); " +
            "} }", clazz));
        clazz.addMethod(CtMethod.make("private void $$ha$loadList0(java.util.List ps, " +
            "org.hotswap.agent.plugin.spring.api.PropertySourceReloader r) throws java.io.IOException { " +
            "for (int i=0;i< ps.size();i++) { " +
            "Object pp = ps.get(i);" +
            " if (pp instanceof org.springframework.core.env.PropertySource) { " +
            "$$ha$load0((org.springframework.core.env.PropertySource)pp,r);" +
            "} } }", clazz));
    }


    private static void makeMapWritable(MethodCall m) throws CannotCompileException {
        if ("java.util.Collections".equals(m.getClassName()) &&
            "unmodifiableMap".equals(m.getMethodName())) {
            m.replace("{ $_ = $1; }");
        }
    }


    private static void makePropertySourceWritable(NewExpr e) {
        try {
            if (e.getClassName().equals("org.springframework.boot.env.OriginTrackedMapPropertySource") &&
                e.getConstructor().getParameterTypes().length == 3) {
                LOGGER.trace("rewrite NewExpr: {}", e.getClassName());
                e.replace("{ $_ = new org.springframework.boot.env.OriginTrackedMapPropertySource($1, $2); }");
            }
        } catch (Exception t) {
            LOGGER.debug("edit NewExpr error", t);
        }
    }
}
