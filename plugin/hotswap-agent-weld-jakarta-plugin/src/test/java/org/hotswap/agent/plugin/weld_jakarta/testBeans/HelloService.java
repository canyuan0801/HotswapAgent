
package org.hotswap.agent.plugin.weld_jakarta.testBeans;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Vetoed;


@Dependent
public abstract class HelloService {
    public abstract String hello();
}
