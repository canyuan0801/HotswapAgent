
package org.hotswap.agent.plugin.spring.annotations.dependent;

import org.springframework.beans.factory.annotation.Autowired;

public class DepTeacherGroup1 {

    @Autowired
    private DepTeacher1 depTeacher1;

    public DepTeacher1 getDepTeacher1() {
        return depTeacher1;
    }

    public void setDepTeacher1(DepTeacher1 depTeacher1) {
        this.depTeacher1 = depTeacher1;
    }
}
