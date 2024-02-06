
package org.hotswap.agent.annotation;

import java.lang.annotation.*;

import static org.hotswap.agent.annotation.LoadEvent.DEFINE;
import static org.hotswap.agent.annotation.LoadEvent.REDEFINE;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OnClassLoadEvent {


    String classNameRegexp();


    LoadEvent[] events() default {DEFINE, REDEFINE};


    boolean skipAnonymous() default true;


    boolean skipSynthetic() default true;

}
