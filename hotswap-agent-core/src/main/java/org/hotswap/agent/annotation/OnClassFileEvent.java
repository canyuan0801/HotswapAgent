
package org.hotswap.agent.annotation;

import java.lang.annotation.*;

import static org.hotswap.agent.annotation.FileEvent.*;



@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OnClassFileEvent {


    String classNameRegexp();


    FileEvent[] events() default {CREATE, MODIFY, DELETE};



    public int timeout() default 50;
}
