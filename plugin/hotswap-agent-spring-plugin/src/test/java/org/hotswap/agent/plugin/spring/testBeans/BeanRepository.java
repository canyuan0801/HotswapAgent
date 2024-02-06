
package org.hotswap.agent.plugin.spring.testBeans;

import org.springframework.stereotype.Repository;


@Repository
public class BeanRepository {
    public String hello() {
        return "Hello from Repository";
    }
}
