
package org.hotswap.agent.plugin.proxy.hscglib;

import java.lang.instrument.IllegalClassFormatException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.plugin.proxy.api.MultistepProxyTransformer;
import org.hotswap.agent.plugin.proxy.api.ProxyBytecodeGenerator;
import org.hotswap.agent.plugin.proxy.api.ProxyBytecodeTransformer;
import org.hotswap.agent.plugin.proxy.api.TransformationState;


public class CglibProxyTransformer extends MultistepProxyTransformer {


    private static final Map<Class<?>, TransformationState> TRANSFORMATION_STATES = Collections
            .synchronizedMap(new WeakHashMap<Class<?>, TransformationState>());
    private GeneratorParams params;


    public CglibProxyTransformer(Class<?> classBeingRedefined,
            ClassPool classPool, byte[] classfileBuffer,
            GeneratorParams params) {
        super(classBeingRedefined, classPool, classfileBuffer,
                TRANSFORMATION_STATES);
        this.params = params;
    }


    public static byte[] transform(Class<?> classBeingRedefined,
            ClassPool classPool, byte[] classfileBuffer, GeneratorParams params)
            throws Exception {
        return new CglibProxyTransformer(classBeingRedefined, classPool,
                classfileBuffer, params).transformRedefine();
    }

    public static boolean isReloadingInProgress() {
        return !TRANSFORMATION_STATES.isEmpty();
    }

    @Override
    protected ProxyBytecodeGenerator createGenerator() {
        return new CglibProxyBytecodeGenerator(params);
    }

    @Override
    protected ProxyBytecodeTransformer createTransformer() {
        return new CglibProxyBytecodeTransformer(classPool);
    }
}
