
package org.hotswap.agent.plugin.spring.annotations.placeholder.annotation1;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StudentConfiguration {
    @Bean
    public Student2 student2() {
        return new Student2();
    }

    @Bean
    public Student3 student3() {
        return new Student3();
    }

}
