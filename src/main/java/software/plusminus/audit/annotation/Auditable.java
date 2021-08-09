package software.plusminus.audit.annotation;

import software.plusminus.audit.model.LogMode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    
    LogMode mode() default LogMode.WRITE;
    
}
