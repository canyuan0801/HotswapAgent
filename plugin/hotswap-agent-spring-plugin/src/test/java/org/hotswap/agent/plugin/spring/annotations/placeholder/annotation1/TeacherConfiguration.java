
package org.hotswap.agent.plugin.spring.annotations.placeholder.annotation1;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TeacherConfiguration {
    @Bean
    public Teacher1 teacher1() {
        return new Teacher1();
    }

    @Bean(name = "teacher2")
    public Teacher2 teacher2(Student2 student2, @Value("${teacher2.name}") String name) {
        return new Teacher2(name, student2);
    }

    @Bean(name = "teacher22")
    public Teacher2 teacher22(@Value("${teacher2.name}") String name) {
        return new Teacher2(name, null);
    }
}
