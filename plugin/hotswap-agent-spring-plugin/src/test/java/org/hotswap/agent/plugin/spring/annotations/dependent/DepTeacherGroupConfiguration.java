
package org.hotswap.agent.plugin.spring.annotations.dependent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DepTeacherGroupConfiguration {
    @Bean
    public DepTeacherGroup2 depTeacherGroup2(DepTeacher2 depTeacher2) {
        return new DepTeacherGroup2(depTeacher2);
    }

    @Bean
    public DepTeacherGroup1 depTeacherGroup1() {
        return new DepTeacherGroup1();
    }

    @Bean
    public DepTeacherGroup3 depTeacherGroup3(DepTeacher3 depTeacher3) {
        return new DepTeacherGroup3(depTeacher3);
    }

    @Bean
    public DepTeacherGroup4 depTeacherGroup4() {
        return new DepTeacherGroup4();
    }

}
