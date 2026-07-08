package dev.basementlab.dealtracker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    // Spring Boot's JacksonAutoConfiguration normally supplies this bean for free, but it's
    // gated on Jackson2ObjectMapperBuilder (a spring-web class) being on the classpath. This
    // app deliberately skips spring-boot-starter-web (no inbound HTTP, no need for embedded
    // Tomcat), so that auto-configuration never fires - hence a plain manual bean here instead.
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
