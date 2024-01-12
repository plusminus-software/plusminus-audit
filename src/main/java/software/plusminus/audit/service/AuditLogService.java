package software.plusminus.audit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.plusminus.audit.exception.AuditException;
import software.plusminus.audit.model.AuditLog;
import software.plusminus.audit.model.DataAction;
import software.plusminus.audit.repository.AuditLogRepository;
import software.plusminus.security.context.SecurityContext;
import software.plusminus.tenant.service.TenantService;
import software.plusminus.util.AnnotationUtils;
import software.plusminus.util.EntityUtils;
import software.plusminus.util.FieldUtils;

import java.time.ZonedDateTime;
import javax.annotation.Nullable;

@Service
public class AuditLogService {

    @Autowired
    private SecurityContext securityContext;
    @Autowired
    private DeviceContext deviceContext;
    @Autowired
    private TransactionContext transactionContext;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private AuditLogService self;
    @Autowired
    private AuditLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public <T> AuditLog<T> logCreate(T entity) {
        AuditLog<T> auditLog = prepareAuditLog(entity, DataAction.CREATE);
        return repository.save(auditLog);
    }

    @Nullable
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> AuditLog<T> logUpdate(T entity) {
        boolean updated = unmarkCurrentAuditLogForEntity(entity);
        if (!updated) {
            return null;
        }
        AuditLog<T> auditLog = prepareAuditLog(entity, DataAction.UPDATE);
        return repository.save(auditLog);
    }

    @Nullable
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> AuditLog<T> logDelete(T entity) {
        boolean updated = unmarkCurrentAuditLogForEntity(entity);
        if (!updated) {
            return null;
        }
        AuditLog<T> auditLog = prepareAuditLog(entity, DataAction.DELETE);
        return repository.save(auditLog);
    }

    @Nullable
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> AuditLog<T> unmarkCurrentAuditLog(String entityType, Long entityId) {
        AuditLog<T> previousAuditLog = repository.findByEntityTypeAndEntityIdAndCurrentTrue(entityType, entityId);
        if (previousAuditLog == null) {
            return null;
        }
        previousAuditLog.setCurrent(false);
        return repository.save(previousAuditLog);
    }

    private <T> AuditLog<T> prepareAuditLog(T entity, DataAction action) {
        AuditLog<T> auditLog = new AuditLog<>();
        auditLog.setEntity(entity);
        auditLog.setTime(ZonedDateTime.now());
        auditLog.setCurrent(true);
        auditLog.setUsername(securityContext.getUsername());
        auditLog.setDevice(deviceContext.currentDevice());
        auditLog.setTransactionId(transactionContext.currentTransactionId());
        if (auditLog.getDevice() == null) {
            auditLog.setDevice("");
        }
        auditLog.setTenant(getTenant(entity));
        auditLog.setAction(action);
        return auditLog;
    }

    private <T> boolean unmarkCurrentAuditLogForEntity(T entity) {
        String entityType = entity.getClass().getName();
        Long entityId = getEntityId(entity);
        AuditLog<T> previousAuditLog = self.unmarkCurrentAuditLog(entityType, entityId);
        return previousAuditLog != null;
    }

    private Long getEntityId(Object entity) {
        Long id = EntityUtils.findId(entity, Long.class);
        if (id == null) {
            throw new AuditException("Can't save AuditLog: entity id is null");
        }
        return id;
    }
    
    private String getTenant(Object entity) {
        String tenant = FieldUtils.readFirst(entity, String.class, 
                field -> AnnotationUtils.isArrayContain(field.getAnnotations(), "Tenant"));
        if (tenant == null) {
            tenant = tenantService.currentTenant();
        }
        return tenant;
    }
}
