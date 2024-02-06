
package org.hotswap.agent.plugin.deltaspike;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.signature.ClassSignatureComparerHelper;
import org.hotswap.agent.util.signature.ClassSignatureElement;


public class DeltaspikeClassSignatureHelper {

    private static AgentLogger LOGGER = AgentLogger.getLogger(DeltaspikeClassSignatureHelper.class);

    private static final ClassSignatureElement[] SIGNATURE_ELEM_PROXY = {
            ClassSignatureElement.SUPER_CLASS,
            ClassSignatureElement.INTERFACES,
            ClassSignatureElement.CLASS_ANNOTATION,
            ClassSignatureElement.CONSTRUCTOR,
            ClassSignatureElement.METHOD,
            ClassSignatureElement.METHOD_ANNOTATION,
            ClassSignatureElement.METHOD_PARAM_ANNOTATION,
            ClassSignatureElement.METHOD_EXCEPTION,
            ClassSignatureElement.FIELD,
            ClassSignatureElement.FIELD_ANNOTATION
    };


    public static String getSignaturePartialBeanClass(Class<?> clazz) {
        try {
            return ClassSignatureComparerHelper.getJavaClassSignature(clazz, SIGNATURE_ELEM_PROXY);
        } catch (Exception e) {
            LOGGER.error("getSignatureForProxyClass(): Error reading signature", e);
            return null;
        }
    }

}