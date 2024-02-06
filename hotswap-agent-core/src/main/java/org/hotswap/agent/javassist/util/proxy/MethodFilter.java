

package org.hotswap.agent.javassist.util.proxy;

import java.lang.reflect.Method;


public interface MethodFilter {

    boolean isHandled(Method m);
}
