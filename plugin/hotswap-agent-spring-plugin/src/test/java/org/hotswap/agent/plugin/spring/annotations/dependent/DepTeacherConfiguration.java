
package org.hotswap.agent.plugin.spring.annotations.dependent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DepTeacherConfiguration {
    @Bean(name = "depTeacher2")
    public DepTeacher2 depTeacher2(DepStudent2 depStudent2) {
        return new DepTeacher2(depStudent2);
    }

    @Bean(name = "depTeacher21")
    public DepTeacher2 depTeacher21(DepStudent2 depStudent2) {
        return new DepTeacher2(depStudent2);
    }

    @Bean
    public DepTeacher1 depTeacher1() {
        return new DepTeacher1();
    }

    @Bean
    public DepTeacher4 depTeacher4() {
        return new DepTeacher4();
    }

    @Bean
    public DepTeacher23Mul depTeacher23Mul(DepStudent2 depStudent2, DepStudent3 depStudent3) {
        return new DepTeacher23Mul(depStudent2, depStudent3);
    }
}
