
package org.hotswap.agent.plugin.mybatis.transformers;

import org.apache.ibatis.javassist.bytecode.AccessFlag;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.javassist.CannotCompileException;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.CtField;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.CtNewMethod;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.mybatis.MyBatisPlugin;
import org.hotswap.agent.plugin.mybatis.proxy.ConfigurationProxy;
import org.hotswap.agent.plugin.mybatis.proxy.SpringMybatisConfigurationProxy;
import org.hotswap.agent.util.PluginManagerInvoker;


public class MyBatisTransformers {

    private static AgentLogger LOGGER = AgentLogger.getLogger(MyBatisTransformers.class);

    public static final String SRC_FILE_NAME_FIELD = "$$ha$srcFileName";
    public static final String REFRESH_DOCUMENT_METHOD = "$$ha$refreshDocument";
    public static final String REFRESH_METHOD = "$$ha$refresh";

    private static final String INITIALIZED_FIELD = "$$ha$initialized";
    private static final String FACTORYBEAN_FIELD = "$$ha$factoryBean";
    public static final String FACTORYBEAN_SET_METHOD = "$$ha$setFactoryBean";
    public static final String CONFIGURATION_PROXY_METHOD = "$$ha$proxySqlSessionFactoryConfiguration";

    @OnClassLoadEvent(classNameRegexp = "org.apache.ibatis.parsing.XPathParser")
    public static void patchXPathParser(CtClass ctClass, ClassPool classPool) throws NotFoundException, CannotCompileException {
        CtClass stringClass = classPool.get("java.lang.String");
        CtField sourceFileField = new CtField(stringClass, SRC_FILE_NAME_FIELD, ctClass);
        ctClass.addField(sourceFileField);

        CtMethod method = ctClass.getDeclaredMethod("createDocument");
        method.insertBefore("{" +
                "this." + SRC_FILE_NAME_FIELD + " = " + org.hotswap.agent.util.IOUtils.class.getName() + ".extractFileNameFromInputSource($1);" +
            "}"
        );
        CtMethod newMethod = CtNewMethod.make(
            "public boolean " + REFRESH_DOCUMENT_METHOD + "() {" +
                "if(this." + SRC_FILE_NAME_FIELD + "!=null) {" +
                    "this.document=createDocument(new org.xml.sax.InputSource(new java.io.FileReader(this." + SRC_FILE_NAME_FIELD + ")));" +
                    "return true;" +
                "}" +
                "return false;" +
            "}", ctClass);
        ctClass.addMethod(newMethod);
        LOGGER.debug("org.apache.ibatis.parsing.XPathParser patched.");
    }

    @OnClassLoadEvent(classNameRegexp = "org.apache.ibatis.builder.BaseBuilder")
    public static void patchBaseBuilder(CtClass ctClass) throws NotFoundException, CannotCompileException {
        LOGGER.debug("org.apache.ibatis.builder.BaseBuilder patched.");
        CtField configField = ctClass.getField("configuration");
        configField.setModifiers(configField.getModifiers() & ~AccessFlag.FINAL);
    }

    @OnClassLoadEvent(classNameRegexp = "org.apache.ibatis.builder.xml.XMLConfigBuilder")
    public static void patchXMLConfigBuilder(CtClass ctClass, ClassPool classPool) throws NotFoundException, CannotCompileException {

        StringBuilder src = new StringBuilder("{");
        src.append(PluginManagerInvoker.buildInitializePlugin(MyBatisPlugin.class));
        src.append(PluginManagerInvoker.buildCallPluginMethod(MyBatisPlugin.class, "registerConfigurationFile",
                XPathParserCaller.class.getName() + ".getSrcFileName(this.parser)", "java.lang.String", "this", "java.lang.Object"));
        src.append("this.configuration = " + ConfigurationProxy.class.getName() + ".getWrapper(this).proxy(this.configuration);");
        src.append("}");

        CtClass[] constructorParams = new CtClass[] {
            classPool.get("org.apache.ibatis.parsing.XPathParser"),
            classPool.get("java.lang.String"),
            classPool.get("java.util.Properties")
        };

        ctClass.getDeclaredConstructor(constructorParams).insertAfter(src.toString());
        CtMethod newMethod = CtNewMethod.make(
            "public void " + REFRESH_METHOD + "() {" +
                "if(" + XPathParserCaller.class.getName() + ".refreshDocument(this.parser)) {" +
                    "this.parsed=false;" +
                    "parse();" +
                "}" +
            "}", ctClass);
        ctClass.addMethod(newMethod);
        LOGGER.debug("org.apache.ibatis.builder.xml.XMLConfigBuilder patched.");
    }

    @OnClassLoadEvent(classNameRegexp = "org.apache.ibatis.builder.xml.XMLMapperBuilder")
    public static void patchXMLMapperBuilder(CtClass ctClass, ClassPool classPool) throws NotFoundException, CannotCompileException {
        StringBuilder src = new StringBuilder("{");
        src.append(PluginManagerInvoker.buildInitializePlugin(MyBatisPlugin.class));
        src.append(PluginManagerInvoker.buildCallPluginMethod(MyBatisPlugin.class, "registerConfigurationFile",
                XPathParserCaller.class.getName() + ".getSrcFileName(this.parser)", "java.lang.String", "this", "java.lang.Object"));
        src.append("}");

        CtClass[] constructorParams = new CtClass[] {
            classPool.get("org.apache.ibatis.parsing.XPathParser"),
            classPool.get("org.apache.ibatis.session.Configuration"),
            classPool.get("java.lang.String"),
            classPool.get("java.util.Map")
        };

        CtConstructor constructor = ctClass.getDeclaredConstructor(constructorParams);
        constructor.insertAfter(src.toString());
        LOGGER.debug("org.apache.ibatis.builder.xml.XMLMapperBuilder patched.");
    }

    @OnClassLoadEvent(classNameRegexp = "org.apache.ibatis.session.SqlSessionFactoryBuilder")
    public static void patchSqlSessionFactoryBuilder(CtClass ctClass, ClassPool classPool) throws NotFoundException, CannotCompileException {

        CtClass objClass = classPool.get("java.lang.Object");
        CtField factoryBeanField = new CtField(objClass, FACTORYBEAN_FIELD, ctClass);
        ctClass.addField(factoryBeanField);

        CtMethod setMethod = CtNewMethod.make(
                "public void " + FACTORYBEAN_SET_METHOD + "(Object factoryBean) {" +
                        "this." + FACTORYBEAN_FIELD + " = factoryBean;" +
                        "}", ctClass);
        ctClass.addMethod(setMethod);

        CtMethod buildMethod = ctClass.getDeclaredMethod("build",
                new CtClass[] {classPool.get("org.apache.ibatis.session.Configuration")});
        buildMethod.insertBefore("{" +
                "if (this." + FACTORYBEAN_FIELD + " != null) {" +
                "config = " + SqlSessionFactoryBeanCaller.class.getName() + ".proxyConfiguration(this." + FACTORYBEAN_FIELD + ", config);" +
                "}" +
                "}"
        );
        LOGGER.debug("org.apache.ibatis.session.SqlSessionFactoryBuilder patched.");
    }

    @OnClassLoadEvent(classNameRegexp = "org.mybatis.spring.SqlSessionFactoryBean")
    public static void patchSqlSessionFactoryBean(CtClass ctClass, ClassPool classPool) throws NotFoundException, CannotCompileException {

        CtClass booleanClass = classPool.get(boolean.class.getName());
        CtField sourceFileField = new CtField(booleanClass, INITIALIZED_FIELD, ctClass);
        ctClass.addField(sourceFileField);

        CtMethod method = ctClass.getDeclaredMethod("afterPropertiesSet");
        method.insertAfter("{" +
                "this." + INITIALIZED_FIELD + " = true;" +
                "}"
        );

        CtConstructor constructor = ctClass.getDeclaredConstructor(new CtClass[] {});
        constructor.insertAfter("{" +
                SqlSessionFactoryBeanCaller.class.getName() + ".setFactoryBean(this.sqlSessionFactoryBuilder, this);" +
                "}");

        CtMethod proxyMethod = CtNewMethod.make(
                "public org.apache.ibatis.session.Configuration " + CONFIGURATION_PROXY_METHOD + "(org.apache.ibatis.session.Configuration configuration) {" +
                        "if(this." + INITIALIZED_FIELD + ") {" +
                        "return configuration;" +
                        "} else {" +
                        "return " + SpringMybatisConfigurationProxy.class.getName() + ".getWrapper(this).proxy(configuration);" +
                        "}" +
                        "}", ctClass);
        ctClass.addMethod(proxyMethod);
        LOGGER.debug("org.mybatis.spring.SqlSessionFactoryBean patched.");
    }
}
