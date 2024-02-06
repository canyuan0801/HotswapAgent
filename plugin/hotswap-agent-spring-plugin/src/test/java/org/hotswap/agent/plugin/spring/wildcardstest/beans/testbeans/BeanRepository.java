
package org.hotswap.agent.plugin.spring.wildcardstest.beans.testbeans;

import org.springframework.stereotype.Repository;


@Repository
public class BeanRepository {
    public String hello() {
        return "Hello from Repository";
    }
}
