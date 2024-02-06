
package org.hotswap.agent.plugin.ibatis;

import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.CtNewMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.PluginManagerInvoker;


public class IBatisTransformers {
    private static AgentLogger LOGGER = AgentLogger.getLogger(IBatisTransformers.class);
    public static final String REFRESH_METHOD = "$$ha$refresh";


    @OnClassLoadEvent(classNameRegexp = "com.ibatis.sqlmap.engine.builder.xml.SqlMapConfigParser")
    public static void patchSqlMapConfigParser(CtClass ctClass, ClassPool classPool) throws CannotCompileException, NotFoundException {
        StringBuilder src = new StringBuilder("{");
        src.append(IBatisConfigurationHandler.class.getName() + ".setSqlMapConfigParser(this);");
        src.append("}");
        CtClass[] constructorParams = new CtClass[] {};
        ctClass.getDeclaredConstructor(constructorParams).insertAfter(src.toString());
        LOGGER.debug("com.ibatis.sqlmap.engine.builder.xml.SqlMapConfigParser patched.");
    }



    @OnClassLoadEvent(classNameRegexp = "org.springframework.orm.ibatis.SqlMapClientFactoryBean")
    public static void patchSqlMapClientFactoryBean(CtClass ctClass, ClassPool classPool) throws CannotCompileException, NotFoundException {
        StringBuilder src = new StringBuilder("{");
        src.append(PluginManagerInvoker.buildInitializePlugin(IBatisPlugin.class));
        src.append(PluginManagerInvoker.buildCallPluginMethod(IBatisPlugin.class, "registConfigFile",IBatisConfigurationHandler.class.getName() + ".toPath(this.configLocations)", "java.lang.String"));
        src.append(PluginManagerInvoker.buildCallPluginMethod(IBatisPlugin.class, "registConfigFile",IBatisConfigurationHandler.class.getName() + ".toPath(this.mappingLocations)", "java.lang.String"));
        src.append(IBatisConfigurationHandler.class.getName() + ".setMapFiles(this.configLocations,this.mappingLocations,this.sqlMapClientProperties);");
        src.append("}");
        CtMethod method = ctClass.getDeclaredMethod("afterPropertiesSet");
        method.insertAfter(src.toString());
        LOGGER.debug("org.springframework.orm.ibatis.SqlMapClientFactoryBean patched.");
    }


    @OnClassLoadEvent(classNameRegexp = "com.ibatis.sqlmap.engine.builder.xml.SqlMapParser")
    public static void  patchSqlMapParser(CtClass ctClass, ClassPool classPool) throws CannotCompileException, NotFoundException {
        StringBuilder src = new StringBuilder("{");
        src.append(IBatisConfigurationHandler.class.getName() + ".setParserState(this.state);");
        src.append("}");
        CtClass[] constructorParams = new CtClass[] {
            classPool.get("com.ibatis.sqlmap.engine.builder.xml.XmlParserState")
        };
        ctClass.getDeclaredConstructor(constructorParams).insertAfter(src.toString());
        LOGGER.debug("com.ibatis.sqlmap.engine.builder.xml.SqlMapParser patched.");
    }


    @OnClassLoadEvent(classNameRegexp = "com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate")
    public static void  patchSqlMapExecutorDelegate(CtClass ctClass, ClassPool classPool) throws CannotCompileException {
        CtMethod newMethod = CtNewMethod.make(
                "public void " + REFRESH_METHOD + "() {" +
                    "this.mappedStatements.clear();" +
                "}", ctClass);
        ctClass.addMethod(newMethod);
        LOGGER.debug("com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate patched.");
    }
}
