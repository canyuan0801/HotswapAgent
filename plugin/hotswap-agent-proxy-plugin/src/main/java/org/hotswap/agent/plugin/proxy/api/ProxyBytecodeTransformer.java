
package org.hotswap.agent.plugin.proxy.api;


public interface ProxyBytecodeTransformer {
    public static final String INIT_FIELD_PREFIX = "initCalled";

    public byte[] transform(byte[] classfileBuffer) throws Exception;

}
