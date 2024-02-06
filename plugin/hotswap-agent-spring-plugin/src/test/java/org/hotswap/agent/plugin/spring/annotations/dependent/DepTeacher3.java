
package org.hotswap.agent.plugin.spring.annotations.dependent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("depTeacher3")
public class DepTeacher3 {

    @Autowired
    public DepTeacher3(DepStudent3 depStudent3) {
        this.student3 = depStudent3;
    }


    private DepStudent3 student3;


    public DepStudent3 getStudent3() {
        return student3;
    }

    public void setStudent3(DepStudent3 student3) {
        this.student3 = student3;
    }
}
