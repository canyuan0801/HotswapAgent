
package org.hotswap.agent.plugin.proxy.hscglib;

import java.lang.instrument.IllegalClassFormatException;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.plugin.proxy.api.ProxyBytecodeGenerator;
import org.hotswap.agent.plugin.proxy.api.ProxyBytecodeTransformer;
import org.hotswap.agent.plugin.proxy.api.SinglestepProxyTransformer;


public class CglibEnhancerProxyTransformer extends SinglestepProxyTransformer {

    private GeneratorParams params;
    private ClassLoader loader;


    public CglibEnhancerProxyTransformer(Class<?> classBeingRedefined,
            ClassPool classPool, byte[] classfileBuffer, ClassLoader loader,
            GeneratorParams params) {
        super(classBeingRedefined, classPool, classfileBuffer);
        this.loader = loader;
        this.params = params;
    }


    public static byte[] transform(Class<?> classBeingRedefined,
            ClassPool classPool, byte[] classfileBuffer, ClassLoader loader,
            GeneratorParams params) throws Exception {
        return new CglibEnhancerProxyTransformer(classBeingRedefined, classPool,
                classfileBuffer, loader, params).transformRedefine();
    }

    @Override
    protected ProxyBytecodeGenerator createGenerator() {
        return new CglibEnhancerProxyBytecodeGenerator(params, loader);
    }

    @Override
    protected ProxyBytecodeTransformer createTransformer() {
        return new CglibProxyBytecodeTransformer(classPool);
    }
}
