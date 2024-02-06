
package org.hotswap.agent.plugin.spring.annotations.dependent;

public class DepTeacherGroup2 {

    public DepTeacherGroup2(DepTeacher2 depTeacher2) {
        this.depTeacher2 = depTeacher2;
    }
    private DepTeacher2 depTeacher2;

    public DepTeacher2 getDepTeacher2() {
        return depTeacher2;
    }

    public void setDepTeacher2(DepTeacher2 depTeacher2) {
        this.depTeacher2 = depTeacher2;
    }
}
