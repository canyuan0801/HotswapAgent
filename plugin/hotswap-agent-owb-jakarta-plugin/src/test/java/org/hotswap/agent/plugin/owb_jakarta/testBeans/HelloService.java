
package org.hotswap.agent.plugin.owb_jakarta.testBeans;

import jakarta.enterprise.context.Dependent;


@Dependent
public interface HelloService {
    public String hello();
}
