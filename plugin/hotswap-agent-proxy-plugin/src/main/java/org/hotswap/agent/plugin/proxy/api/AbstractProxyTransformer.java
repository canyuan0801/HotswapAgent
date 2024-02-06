
package org.hotswap.agent.plugin.proxy.api;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.plugin.proxy.ProxyClassSignatureHelper;


public abstract class AbstractProxyTransformer implements ProxyTransformer
{
    public AbstractProxyTransformer(Class<?> classBeingRedefined, ClassPool classPool) {
        super();
        this.classBeingRedefined = classBeingRedefined;
        this.classPool = classPool;
    }

    protected ProxyBytecodeGenerator generator;
    protected ProxyBytecodeTransformer transformer;
    protected Class<?> classBeingRedefined;
    protected ClassPool classPool;

    protected ProxyBytecodeGenerator getGenerator() {
        if (generator == null) {
            generator = createGenerator();
        }
        return generator;
    }

    protected ProxyBytecodeTransformer getTransformer() {
        if (transformer == null) {
            transformer = createTransformer();
        }
        return transformer;
    }

    
    protected abstract ProxyBytecodeGenerator createGenerator();

    
    protected abstract ProxyBytecodeTransformer createTransformer();

    
    protected boolean isTransformingNeeded() {
        return ProxyClassSignatureHelper.isNonSyntheticPoolClassOrParentDifferent(classBeingRedefined, classPool);
    }

}