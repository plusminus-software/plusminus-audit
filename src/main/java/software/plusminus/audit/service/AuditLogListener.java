package software.plusminus.audit.service;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import software.plusminus.audit.annotation.Auditable;
import software.plusminus.listener.DataAction;
import software.plusminus.listener.JoinPoint;
import software.plusminus.listener.WriteListener;

@AllArgsConstructor
@Component
@ConditionalOnBean(AuditLogService.class)
public class AuditLogListener implements WriteListener<Object> {

    private AuditLogService service;

    @Override
    public JoinPoint joinPoint() {
        return JoinPoint.AFTER;
    }

    @Override
    public boolean supports(Object object) {
        return AnnotationUtils.findAnnotation(object.getClass(), Auditable.class) != null;
    }

    @Override
    public void onWrite(Object object, DataAction action) {
        service.log(object, action);
    }
}
