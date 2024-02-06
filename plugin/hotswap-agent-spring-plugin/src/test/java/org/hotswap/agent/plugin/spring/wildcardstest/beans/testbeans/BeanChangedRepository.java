
package org.hotswap.agent.plugin.spring.wildcardstest.beans.testbeans;

import org.springframework.stereotype.Repository;


@Repository
public class BeanChangedRepository {
    public String hello() {
        return "Hello from ChangedRepository";
    }
}
