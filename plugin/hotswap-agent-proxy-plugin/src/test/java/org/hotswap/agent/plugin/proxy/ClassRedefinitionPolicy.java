
package org.hotswap.agent.plugin.proxy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(value = RetentionPolicy.RUNTIME)
public @interface ClassRedefinitionPolicy {

    
    public final static class NoClass {
    }

    Class<?> alias() default NoClass.class;
}
