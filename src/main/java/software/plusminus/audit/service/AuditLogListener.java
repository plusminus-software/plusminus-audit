package software.plusminus.audit.service;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import software.plusminus.audit.annotation.Auditable;
import software.plusminus.crud.CrudAction;
import software.plusminus.crud.listener.CrudJoinpoint;
import software.plusminus.crud.listener.WriteListener;
import software.plusminus.listener.Joinpoint;

@AllArgsConstructor
@Component
@ConditionalOnBean(AuditLogService.class)
public class AuditLogListener implements WriteListener<Object> {

    private AuditLogService service;

    @Override
    public Joinpoint joinpoint() {
        return CrudJoinpoint.AFTER;
    }

    @Override
    public boolean supports(Object object) {
        return AnnotationUtils.findAnnotation(object.getClass(), Auditable.class) != null;
    }

    @Override
    public void onWrite(Object object, CrudAction action) {
        service.log(object, action);
    }
}
