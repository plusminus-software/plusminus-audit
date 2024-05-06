package software.plusminus.audit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
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
import java.util.UUID;
import javax.persistence.EntityManager;

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
    private AuditLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public <T> AuditLog<T> log(EntityManager entityManager, T entity, DataAction action) {
        String entityType = entity.getClass().getName();
        Long entityId = getEntityId(entity, action);
        UUID transactionId = transactionContext.currentTransactionId();
        AuditLog<T> alreadyAdded = isAlreadyPersisted(entityManager, transactionId,
                entityType, entityId, action);
        if (alreadyAdded != null) {
            return alreadyAdded;
        }
        if (action != DataAction.CREATE) {
            unmarkCurrentAuditLogForEntity(entityManager, entityType, entityId);
        }
        AuditLog<T> auditLog = prepareAuditLog(entity, action, transactionId);
        entityManager.persist(auditLog);
        return auditLog;
    }

    private <T> AuditLog<T> prepareAuditLog(T entity, DataAction action, UUID transactionId) {
        AuditLog<T> auditLog = new AuditLog<>();
        auditLog.setEntity(entity);
        auditLog.setTime(ZonedDateTime.now());
        auditLog.setCurrent(true);
        auditLog.setUsername(securityContext.getUsername());
        auditLog.setDevice(deviceContext.currentDevice());
        auditLog.setTransactionId(transactionId);
        if (auditLog.getDevice() == null) {
            auditLog.setDevice("");
        }
        auditLog.setTenant(getTenant(entity));
        auditLog.setAction(action);
        return auditLog;
    }

    @Nullable
    private <T> AuditLog<T> isAlreadyPersisted(EntityManager entityManager, @Nullable UUID transactionId,
                                               String entityType, @Nullable Long entityId,
                                               DataAction action) {
        if (transactionId == null || entityId == null) {
            return null;
        }
        AuditLog<T> sameTransactionAuditLog = repository.findByEntityTypeAndEntityIdAndTransactionId(
                entityType, entityId, transactionId);
        if (sameTransactionAuditLog == null) {
            return null;
        }
        updateSameTransactionAuditLogIfNeeded(entityManager, sameTransactionAuditLog, action);
        return sameTransactionAuditLog;
    }

    private void updateSameTransactionAuditLogIfNeeded(EntityManager entityManager, AuditLog<?> present,
                                                       DataAction action) {
        if (present.getAction() == action) {
            return;
        }
        if (present.getAction() == DataAction.CREATE && action == DataAction.UPDATE
                || present.getAction() == DataAction.UPDATE && action == DataAction.CREATE) {
            return;
        }
        entityManager.createQuery(
                "update AuditLog a set a.action = ?1 where number = ?2")
                .setParameter(1, action)
                .setParameter(2, present.getNumber())
                .executeUpdate();
    }

    private void unmarkCurrentAuditLogForEntity(EntityManager entityManager, String entityType,
                                                Long entityId) {
        entityManager.createQuery(
                "update AuditLog a set a.current = false where entityType = ?1 and entityId = ?2")
                .setParameter(1, entityType)
                .setParameter(2, entityId)
                .executeUpdate();
    }

    @Nullable
    private Long getEntityId(Object entity, DataAction action) {
        Long id = EntityUtils.findId(entity, Long.class);
        if (id == null) {
            if (action == DataAction.CREATE) {
                return null;
            }
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
