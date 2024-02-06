
package org.hotswap.agent.plugin.spring.testBeans;

import org.springframework.stereotype.Repository;


@Repository
public class BeanOtherRepository {
    public String hello() {
        return "Hello from ChangedRepository";
    }
}
