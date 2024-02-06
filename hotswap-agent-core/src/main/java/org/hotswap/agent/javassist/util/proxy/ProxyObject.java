

package org.hotswap.agent.javassist.util.proxy;


public interface ProxyObject extends Proxy {

    @Override
    void setHandler(MethodHandler mi);


    MethodHandler getHandler();
}
