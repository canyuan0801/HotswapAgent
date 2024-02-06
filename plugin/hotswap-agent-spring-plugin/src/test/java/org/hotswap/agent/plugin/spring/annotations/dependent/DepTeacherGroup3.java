
package org.hotswap.agent.plugin.spring.annotations.dependent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DepTeacherGroup3 {

    @Autowired
    public DepTeacherGroup3(DepTeacher3 depTeacher3) {
        this.depTeacher3 = depTeacher3;
    }


    private DepTeacher3 depTeacher3;

    public DepTeacher3 getDepTeacher3() {
        return depTeacher3;
    }

    public void setDepTeacher3(DepTeacher3 depTeacher3) {
        this.depTeacher3 = depTeacher3;
    }
}
