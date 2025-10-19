package software.plusminus.audit.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import software.plusminus.audit.model.AuditLog;
import software.plusminus.context.Context;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ComponentScan("software.plusminus.audit")
@EntityScan("software.plusminus.audit")
@EnableJpaRepositories("software.plusminus.audit")
public class AuditAutoconfig {

    @Bean
    Context<List<AuditLog<?>>> auditLogContext() {
        return Context.of(ArrayList::new);
    }
}
