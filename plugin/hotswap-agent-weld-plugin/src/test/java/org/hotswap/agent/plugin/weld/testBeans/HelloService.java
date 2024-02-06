
package org.hotswap.agent.plugin.weld.testBeans;

import javax.enterprise.context.Dependent;


@Dependent
public interface HelloService {
    public String hello();
}
