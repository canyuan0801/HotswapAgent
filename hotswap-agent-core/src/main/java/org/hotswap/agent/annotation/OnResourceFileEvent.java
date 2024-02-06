
package org.hotswap.agent.annotation;

import java.lang.annotation.*;

import static org.hotswap.agent.annotation.FileEvent.*;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OnResourceFileEvent {


    String path();


    String filter() default "";


    FileEvent[] events() default {CREATE, MODIFY, DELETE};


    public boolean onlyRegularFiles() default true;


    public int timeout() default 50;
}