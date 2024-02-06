
package org.hotswap.agent.annotation;

import java.lang.annotation.*;


@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Plugin {


    String name() default "";


    String description() default "";


    String group() default "";


    boolean fallback() default false;


    String[] testedVersions();


    String[] expectedVersions() default {};


    Class<?>[] supportClass() default {};

}
