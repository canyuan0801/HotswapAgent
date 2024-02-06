
package org.hotswap.agent.plugin.spring.annotations.dependent;

import org.springframework.beans.factory.annotation.Autowired;

public class DepTeacher4 {

    @Autowired
    private DepStudent4 depStudent4;

    public DepStudent4 getStudent4() {
        return depStudent4;
    }
}
