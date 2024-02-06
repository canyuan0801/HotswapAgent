
package org.hotswap.agent.util;

import org.hotswap.agent.config.PluginManager;

import java.lang.reflect.Method;


public class PluginManagerInvoker {


    public static <T> T callInitializePlugin(Class<T> pluginClass, ClassLoader appClassLoader) {

        return (T) PluginManager.getInstance().getPluginRegistry().initializePlugin(
                pluginClass.getName(), appClassLoader
        );
    }


    public static String buildInitializePlugin(Class pluginClass) {
        return buildInitializePlugin(pluginClass, "getClass().getClassLoader()");
    }

    public static String buildInitializePlugin(Class pluginClass, String classLoaderVar) {
        return "org.hotswap.agent.config.PluginManager.getInstance().getPluginRegistry().initializePlugin(" +
                "\"" + pluginClass.getName() + "\", " + classLoaderVar +
                ");";
    }



    public static void callCloseClassLoader(ClassLoader appClassLoader) {
        PluginManager.getInstance().closeClassLoader(appClassLoader);
    }

    public static String buildCallCloseClassLoader(String classLoaderVar) {
        return "org.hotswap.agent.config.PluginManager.getInstance().closeClassLoader(" + classLoaderVar + ");";
    }


    public static Object callPluginMethod(Class pluginClass, ClassLoader appClassLoader, String method, Class[] paramTypes, Object[] params) {
        Object pluginInstance = PluginManager.getInstance().getPlugin(pluginClass.getName(), appClassLoader);

        try {
            Method m = pluginInstance.getClass().getDeclaredMethod(method, paramTypes);
            return m.invoke(pluginInstance, params);
        } catch (Exception e) {
            throw new Error(String.format("Exception calling method %s on plugin class %s", method, pluginClass), e);
        }
    }


    public static String buildCallPluginMethod(Class pluginClass, String method, String... paramValueAndType) {
        return buildCallPluginMethod("getClass().getClassLoader()", pluginClass, method, paramValueAndType);
    }


    public static String buildCallPluginMethod(String appClassLoaderVar, Class pluginClass,
                                               String method, String... paramValueAndType) {

        String managerClass = PluginManager.class.getName();
        int paramCount = paramValueAndType.length / 2;

        StringBuilder b = new StringBuilder();


        b.append("try {");

        b.append("ClassLoader __pluginClassLoader = ");
        b.append(managerClass);
        b.append(".class.getClassLoader();");


        b.append("Object __pluginInstance = ");
        b.append(managerClass);
        b.append(".getInstance().getPlugin(");
        b.append(pluginClass.getName());
        b.append(".class.getName(), " + appClassLoaderVar + ");");


        b.append("Class __pluginClass = ");
        b.append("__pluginClassLoader.loadClass(\"");
        b.append(pluginClass.getName());
        b.append("\");");


        b.append("Class[] paramTypes = new Class[" + paramCount + "];");
        for (int i = 0; i < paramCount; i++) {

            b.append("paramTypes[" + i + "] = __pluginClassLoader.loadClass(\"" + paramValueAndType[(i * 2) + 1] + "\");");
        }


        b.append("java.lang.reflect.Method __callPlugin = __pluginClass.getDeclaredMethod(\"");
        b.append(method);
        b.append("\", paramTypes");
        b.append(");");

        b.append("Object[] params = new Object[" + paramCount + "];");
        for (int i = 0; i < paramCount; i = i + 1) {
            b.append("params[" + i + "] = " + paramValueAndType[i * 2] + ";");
        }


        b.append("__callPlugin.invoke(__pluginInstance, params);");


        b.append("} catch (Exception e) {throw new Error(e);}");

        return b.toString();
    }
}
