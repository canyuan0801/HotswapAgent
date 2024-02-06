
package org.hotswap.agent.plugin.spring.annotations.dependent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


public class DepTeacher1 {

    @Autowired
    private DepStudent1 depStudent1;

    public DepStudent1 getStudent1() {
        return depStudent1;
    }

    public void setStudent1(DepStudent1 student1) {
        this.depStudent1 = student1;
    }
}
