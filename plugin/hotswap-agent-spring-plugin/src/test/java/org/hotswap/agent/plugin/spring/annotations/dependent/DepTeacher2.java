
package org.hotswap.agent.plugin.spring.annotations.dependent;

public class DepTeacher2 {

    public DepTeacher2(DepStudent2 student2) {
        this.student2 = student2;
    }
    private DepStudent2 student2;

    public DepStudent2 getStudent2() {
        return student2;
    }

    public void setStudent2(DepStudent2 student2) {
        this.student2 = student2;
    }
}
