
package org.hotswap.agent.annotation;

import java.lang.annotation.*;


@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Init {
}
