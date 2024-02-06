
package org.hotswap.agent.plugin.spring.annotations.dependent;

public class DepTeacher23Mul {

    public DepTeacher23Mul(DepStudent2 student2, DepStudent3 student3) {
        this.student2 = student2;
        this.student3 = student3;
    }
    private DepStudent2 student2;
    private DepStudent3 student3;

    public DepStudent2 getStudent2() {
        return student2;
    }

    public void setStudent2(DepStudent2 student2) {
        this.student2 = student2;
    }

    public DepStudent3 getStudent3() {
        return student3;
    }

    public void setStudent3(DepStudent3 student3) {
        this.student3 = student3;
    }
}
