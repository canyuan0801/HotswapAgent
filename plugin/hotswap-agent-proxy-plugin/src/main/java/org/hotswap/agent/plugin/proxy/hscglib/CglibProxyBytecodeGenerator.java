
package org.hotswap.agent.plugin.proxy.hscglib;

import java.lang.reflect.Method;

import org.hotswap.agent.plugin.proxy.api.ProxyBytecodeGenerator;


public class CglibProxyBytecodeGenerator implements ProxyBytecodeGenerator {
    private GeneratorParams params;

    public CglibProxyBytecodeGenerator(GeneratorParams params) {
        super();
        this.params = params;
    }

    public byte[] generate() throws Exception {
        Method genMethod = getGenerateMethod(params.getGenerator());
        if (genMethod == null)
            throw new RuntimeException(
                    "No generation Method found for redefinition!");
        return (byte[]) genMethod.invoke(params.getGenerator(),
                params.getParam());
    }


    private Method getGenerateMethod(Object generator) {
        Method[] methods = generator.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals("generate")
                    && method.getReturnType().getSimpleName().equals("byte[]")
                    && method.getParameterTypes().length == 1) {
                return method;
            }
        }
        return null;
    }
}
