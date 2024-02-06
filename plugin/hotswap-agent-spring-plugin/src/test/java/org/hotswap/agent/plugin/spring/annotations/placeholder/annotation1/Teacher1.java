
package org.hotswap.agent.plugin.spring.annotations.placeholder.annotation1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class Teacher1 {
    @Value("${teacher.name}")
    private String name;

    @Autowired
    private Student1 student1;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
