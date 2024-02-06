
package org.hotswap.agent.plugin.owb;

import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.signature.ClassSignatureComparerHelper;
import org.hotswap.agent.util.signature.ClassSignatureElement;


public class OwbClassSignatureHelper {

    private static AgentLogger LOGGER = AgentLogger.getLogger(OwbClassSignatureHelper.class);

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

    private static final ClassSignatureElement[] SIGNATURE_ELEM_METHOD_FIELDS = {
            ClassSignatureElement.SUPER_CLASS,
            ClassSignatureElement.INTERFACES,
            ClassSignatureElement.CLASS_ANNOTATION,
            ClassSignatureElement.CONSTRUCTOR,
            ClassSignatureElement.CONSTRUCTOR_PRIVATE, 
            ClassSignatureElement.METHOD,
            ClassSignatureElement.METHOD_PRIVATE, 
            ClassSignatureElement.METHOD_ANNOTATION, 
            ClassSignatureElement.METHOD_PARAM_ANNOTATION, 
            ClassSignatureElement.FIELD,
            ClassSignatureElement.FIELD_ANNOTATION
    };

    private static final ClassSignatureElement[] SIGNATURE_ELEM_FIELDS = {
            ClassSignatureElement.FIELD,
            ClassSignatureElement.FIELD_ANNOTATION
    };

    
    public static String getSignatureForProxyClass(Class<?> clazz) {
        try {
            return ClassSignatureComparerHelper.getJavaClassSignature(clazz, SIGNATURE_ELEM_PROXY);
        } catch (Exception e) {
            LOGGER.error("getSignatureForProxyClass(): Error reading signature", e);
            return null;
        }
    }

    
    public static String getSignatureByStrategy(BeanReloadStrategy strategy, Class<?> clazz) {
        if (strategy == null) {
            strategy = BeanReloadStrategy.NEVER;
        }
        switch (strategy) {
        case CLASS_CHANGE :
            return null;
        case METHOD_FIELD_SIGNATURE_CHANGE :
            return getClassMethodFieldsSignature(clazz);
        case FIELD_SIGNATURE_CHANGE :
            return getClassFieldsSignature(clazz);
        default:
        case NEVER:
            return null;
        }
    }

    private static String getClassMethodFieldsSignature(Class<?> clazz) {
        try {
            return ClassSignatureComparerHelper.getJavaClassSignature(clazz, SIGNATURE_ELEM_METHOD_FIELDS);
        } catch (Exception e) {
            LOGGER.error("getSignatureForProxyClass(): Error reading signature", e);
            return null;
        }
    }

    private static String getClassFieldsSignature(Class<?> clazz) {
        try {
            return ClassSignatureComparerHelper.getJavaClassSignature(clazz, SIGNATURE_ELEM_FIELDS);
        } catch (Exception e) {
            LOGGER.error("getSignatureForProxyClass(): Error reading signature", e);
            return null;
        }
    }
}
