
package org.hotswap.agent.plugin.spring.annotations.placeholder.annotation1;

public class Teacher2 {

    public Teacher2(String name, Student2 student2) {
        this.name = name;
        this.student2 = student2;
    }

    private String name;
    private Student2 student2;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
