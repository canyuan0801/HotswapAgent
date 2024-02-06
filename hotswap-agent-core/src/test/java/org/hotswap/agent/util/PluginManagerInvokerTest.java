
package org.hotswap.agent.util;

import org.hotswap.agent.config.PluginManager;
import org.hotswap.agent.config.PluginRegistry;
import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtNewMethod;
import org.hotswap.agent.testData.SimplePlugin;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;


public class PluginManagerInvokerTest {

    @Test
    public void testBuildCallPluginMethod() throws Exception {
        SimplePlugin plugin = new SimplePlugin();
        registerPlugin(plugin);


        String s = PluginManagerInvoker.buildCallPluginMethod(plugin.getClass(),
                "callPluginMethod",
                "Boolean.TRUE", "java.lang.Boolean");

        ClassPool classPool = ClassPool.getDefault();
        classPool.appendSystemPath();

        CtClass clazz = classPool.makeClass("Test");
        clazz.addMethod(CtNewMethod.make("public void test() {" + s + "}", clazz));
        Class<?> testClass = clazz.toClass();


        Method testMethod = testClass.getDeclaredMethod("test");
        testMethod.invoke(testClass.newInstance());


    }

    
    private void registerPlugin(Object plugin) throws NoSuchFieldException, IllegalAccessException {
        Field f = PluginRegistry.class.getDeclaredField("registeredPlugins");
        f.setAccessible(true);
        
        Map<Class, Map<ClassLoader, Object>> registeredPlugins =
                (Map<Class, Map<ClassLoader, Object>>) f.get(PluginManager.getInstance().getPluginRegistry());
        registeredPlugins.put(plugin.getClass(), Collections.singletonMap(getClass().getClassLoader(), plugin));
    }
}
