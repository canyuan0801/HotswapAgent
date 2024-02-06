
package org.hotswap.agent.plugin.spring.annotations.dependentbak;

import org.hotswap.agent.plugin.spring.annotations.dependent.DepStudent4;
import org.springframework.beans.factory.annotation.Autowired;

public class DepBakTeacher4 {

    @Autowired
    private DepStudent4 depStudent4;

    public DepStudent4 getStudent4() {
        return depStudent4;
    }
}
