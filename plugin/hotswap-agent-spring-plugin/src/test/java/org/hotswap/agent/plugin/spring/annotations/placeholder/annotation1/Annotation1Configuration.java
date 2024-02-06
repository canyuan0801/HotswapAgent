package org.hotswap.agent.plugin.spring.annotations.placeholder.annotation1;

import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@ComponentScan
@PropertySource(value = "classpath:annotation-configuration/configuration-item.properties")
public class Annotation1Configuration {
    
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
