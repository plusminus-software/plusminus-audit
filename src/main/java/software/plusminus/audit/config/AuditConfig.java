package software.plusminus.audit.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan("software.plusminus.audit")
@EntityScan("software.plusminus.audit")
@EnableJpaRepositories("software.plusminus.audit")
public class AuditConfig {
}
