
package org.hotswap.agent.util.signature;

import org.hotswap.agent.javassist.ClassPool;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.NotFoundException;
import org.hotswap.agent.logging.AgentLogger;


public class ClassSignatureComparerHelper {

    private static AgentLogger LOGGER = AgentLogger.getLogger(ClassSignatureComparerHelper.class);

    public static String getCtClassSignature(CtClass ctClass, ClassSignatureElement[] signatureElements) throws Exception {
        CtClassSignature signature = new CtClassSignature(ctClass);
        signature.addSignatureElements(signatureElements);
        return signature.getValue();
    }

    public static String getJavaClassSignature(Class<?> clazz, ClassSignatureElement[] signatureElements) throws Exception  {
        JavaClassSignature signature = new JavaClassSignature(clazz);
        signature.addSignatureElements(signatureElements);
        return signature.getValue();
    }


    public static boolean isDifferent(CtClass ctClass, Class<?> clazz, ClassSignatureElement[] signatureElements) {
        try {
            String sig1 = getCtClassSignature(ctClass, signatureElements);
            String sig2 = getJavaClassSignature(clazz, signatureElements);
            return !sig1.equals(sig2);
        } catch (Exception e) {
            LOGGER.error("Error reading signature", e);
            return false;
        }
    }

    public static boolean isDifferent(Class<?> clazz1, Class<?> clazz2, ClassSignatureElement[] signatureElements) {
        try {
            String sig1 = getJavaClassSignature(clazz1, signatureElements);
            String sig2 = getJavaClassSignature(clazz2, signatureElements);
            return !sig1.equals(sig2);
        } catch (Exception e) {
            LOGGER.error("Error reading signature", e);
            return false;
        }
    }


    public static boolean isPoolClassDifferent(Class<?> clazz, ClassPool cp, ClassSignatureElement[] signatureElements) {
        try {
            return isDifferent(cp.get(clazz.getName()), clazz, signatureElements);
        } catch (NotFoundException e) {
            LOGGER.error("Class not found ", e);
            return false;
        }
    }

}
