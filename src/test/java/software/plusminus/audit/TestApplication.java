package software.plusminus.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import software.plusminus.audit.config.AuditAutoconfig;
import software.plusminus.context.Context;
import software.plusminus.security.Security;

@SpringBootApplication
@Import(AuditAutoconfig.class)
public class TestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @Bean
    Context<Security> securityContext() {
        return Context.of(() -> Security.builder()
                .username("TestUser")
                .build());
    }
}
