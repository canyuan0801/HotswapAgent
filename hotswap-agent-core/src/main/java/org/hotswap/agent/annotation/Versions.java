
package org.hotswap.agent.annotation;


import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Versions {


    Maven[] maven() default {};


    Manifest[] manifest() default {};
}
