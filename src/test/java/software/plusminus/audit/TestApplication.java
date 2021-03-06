package software.plusminus.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import software.plusminus.audit.config.AuditAutoconfig;

@SpringBootApplication
@Import(AuditAutoconfig.class)
public class TestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
