
package org.hotswap.agent.plugin.proxy.api;

import java.util.Map;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.proxy.RedefinitionScheduler;


public abstract class MultistepProxyTransformer extends AbstractProxyTransformer
{
    private static final AgentLogger LOGGER = AgentLogger.getLogger(MultistepProxyTransformer.class);
    public static boolean addThirdStep = false;

    protected byte[] classfileBuffer;
    protected Map<Class<?>, TransformationState> transformationStates;
    protected ProxyBytecodeGenerator generator;
    protected ProxyBytecodeTransformer transformer;

    public MultistepProxyTransformer(Class<?> classBeingRedefined, ClassPool classPool, byte[] classfileBuffer,
            Map<Class<?>, TransformationState> transformationStates) {
        super(classBeingRedefined, classPool);
        this.classPool = classPool;
        this.transformationStates = transformationStates;
        this.classfileBuffer = classfileBuffer;
    }


    public byte[] transformRedefine() throws Exception {
        switch (getTransformationstate()) {
            case NEW:
                if (!isTransformingNeeded()) {
                    return classfileBuffer;
                }
                setClassAsWaiting();


                scheduleRedefinition();
                return classfileBuffer;
            case WAITING:
                classfileBuffer = getTransformer().transform(getGenerator().generate());
                LOGGER.reload("Class '{}' has been reloaded.", classBeingRedefined.getName());
                if (addThirdStep) {
                    setClassAsFinished();
                    scheduleRedefinition();
                } else
                    removeClassState();
                return classfileBuffer;
            case FINISHED:
                removeClassState();
                return classfileBuffer;
            default:
                throw new RuntimeException("Unhandeled TransformationState!");
        }
    }


    protected TransformationState getTransformationstate() {
        TransformationState transformationState = transformationStates.get(classBeingRedefined);
        if (transformationState == null)
            transformationState = TransformationState.NEW;
        return transformationState;
    }


    protected void scheduleRedefinition() {
        RedefinitionScheduler.schedule(this);
    }


    protected TransformationState setClassAsWaiting() {
        return transformationStates.put(classBeingRedefined, TransformationState.WAITING);
    }


    protected TransformationState setClassAsFinished() {
        return transformationStates.put(classBeingRedefined, TransformationState.FINISHED);
    }


    public TransformationState removeClassState() {
        return transformationStates.remove(classBeingRedefined);
    }


    public Class<?> getClassBeingRedefined() {
        return classBeingRedefined;
    }


    public byte[] getClassfileBuffer() {
        return classfileBuffer;
    }
}
