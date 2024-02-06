
package org.hotswap.agent.plugin.proxy.api;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.logging.AgentLogger;


public abstract class SinglestepProxyTransformer extends AbstractProxyTransformer
{
    private static final AgentLogger LOGGER = AgentLogger.getLogger(SinglestepProxyTransformer.class);

    protected byte[] classfileBuffer;

    public SinglestepProxyTransformer(Class<?> classBeingRedefined, ClassPool classPool, byte[] classfileBuffer) {
        super(classBeingRedefined, classPool);
        this.classfileBuffer = classfileBuffer;
    }


    public byte[] transformRedefine() throws Exception {
        if (!isTransformingNeeded()) {
            return classfileBuffer;
        }
        classfileBuffer = getTransformer().transform(getGenerator().generate());
        LOGGER.reload("Class '{}' has been reloaded.", classBeingRedefined.getName());
        return classfileBuffer;
    }
}
