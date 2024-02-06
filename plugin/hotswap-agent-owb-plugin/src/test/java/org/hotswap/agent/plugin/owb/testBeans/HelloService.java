
package org.hotswap.agent.plugin.owb.testBeans;

import javax.enterprise.context.Dependent;


@Dependent
public interface HelloService {
    public String hello();
}
