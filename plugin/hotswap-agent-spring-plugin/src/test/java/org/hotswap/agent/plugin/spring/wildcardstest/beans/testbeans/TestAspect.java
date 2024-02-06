
package org.hotswap.agent.plugin.spring.wildcardstest.beans.testbeans;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;


@Aspect
public class TestAspect {
    @Around("execution(* org.hotswap.agent.plugin.spring.wildcardstest.beans.testbeans.*Service.hello(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed() + "WithAspect";
    }
}
