
package org.hotswap.agent.versions.matcher;

import java.lang.reflect.Method;

import org.hotswap.agent.annotation.Versions;


public class MethodMatcher extends AbstractMatcher {


    public MethodMatcher(Method method) {
        super(method.getAnnotation(Versions.class));
    }
}
