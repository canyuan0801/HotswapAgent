
package org.hotswap.agent.plugin.spring.annotations.dependent;

import org.hotswap.agent.plugin.spring.annotations.placeholder.annotation1.Student2;
import org.hotswap.agent.plugin.spring.annotations.placeholder.annotation1.Student3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class DepStudentConfiguration {
    @Bean
    public DepStudent2 depStudent2() {
        return new DepStudent2();
    }

    @Bean
    public DepStudent3 depStudent3() {
        return new DepStudent3();
    }

    @Bean
    public DepStudent4 depStudent4() {
        return new DepStudent4();
    }
}
