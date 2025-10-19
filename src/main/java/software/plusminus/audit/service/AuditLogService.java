package software.plusminus.audit.service;

import lombok.AllArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.plusminus.audit.exception.AuditException;
import software.plusminus.audit.model.AuditLog;
import software.plusminus.audit.repository.AuditLogRepository;
import software.plusminus.audit.util.AuditLogUtil;
import software.plusminus.context.Context;
import software.plusminus.listener.DataAction;
import software.plusminus.security.context.SecurityContext;
import software.plusminus.tenant.service.TenantService;
import software.plusminus.util.AnnotationUtils;
import software.plusminus.util.EntityUtils;
import software.plusminus.util.FieldUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class AuditLogService {

    private Context<List<AuditLog<?>>> auditLogContext;
    private SecurityContext securityContext;
    private DeviceContext deviceContext;
    private TransactionContext transactionContext;
    private TenantService tenantService;
    private AuditLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public <T> AuditLog<T> log(T entity, DataAction action) {
        String entityType = entity.getClass().getName();
        Long entityId = getEntityId(entity, action);
        UUID transactionId = transactionContext.currentTransactionId();
        AuditLog<T> presentInTheContext = findInContext(entityType, entityId, transactionId, action);
        if (presentInTheContext != null) {
            updatePresentInContext(presentInTheContext, action);
            return presentInTheContext;
        }
        unmarkCurrentAuditLogForEntity(entityType, entityId, action);
        AuditLog<T> auditLog = prepareAuditLog(entity, action, transactionId);
        auditLogContext.get().add(auditLog);
        repository.save(auditLog);
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

    private void unmarkCurrentAuditLogForEntity(String entityType, Long entityId, DataAction action) {
        if (action == DataAction.CREATE) {
            return;
        }
        repository.findByEntityTypeAndEntityIdAndCurrentTrue(entityType, entityId)
                        .forEach(auditLog -> {
                            auditLog.setCurrent(false);
                            repository.save(auditLog);
                        });
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

    @Nullable
    private <T> AuditLog<T> findInContext(String entityType, Long entityId, UUID transactionId,
                                          DataAction dataAction) {
        List<AuditLog<T>> presentInContext = auditLogContext.get().stream()
                .filter(auditLog -> {
                    String presentEntityType = auditLog.getEntity().getClass().getName();
                    Long presentEntityId = getEntityId(auditLog.getEntity(), dataAction);
                    return Objects.equals(presentEntityType, entityType)
                            && Objects.equals(presentEntityId, entityId);
                })
                .map(auditLog -> (AuditLog<T>) auditLog)
                .collect(Collectors.toList());
        if (presentInContext.isEmpty()) {
            return null;
        }
        if (presentInContext.size() > 1) {
            throw new AuditException("More than one AuditLog present in the context with entityType "
                    + entityType + " and entityId " + entityId);
        }
        if (!Objects.equals(presentInContext.get(0).getTransactionId(), transactionId)) {
            throw new AuditException("The AuditLog present in the context with entityType "
                    + entityType + " and entityId " + entityId + " has different transactionId");
        }
        return presentInContext.get(0);
    }

    private <T> void updatePresentInContext(AuditLog<T> presentInContext, DataAction newAction) {
        if (presentInContext.getAction() == newAction) {
            return;
        }
        if (presentInContext.getAction() == DataAction.CREATE
                && newAction == DataAction.UPDATE) {
            return;
        }
        if (presentInContext.getAction() == DataAction.CREATE
                && newAction == DataAction.DELETE) {
            auditLogContext.get().remove(presentInContext);
            repository.delete(presentInContext);
            return;
        }
        if (presentInContext.getAction() == DataAction.UPDATE
                && newAction == DataAction.DELETE) {
            presentInContext.setAction(DataAction.DELETE);
            repository.save(presentInContext);
            return;
        }
        if (presentInContext.getAction() == DataAction.DELETE
                && newAction == DataAction.UPDATE) {
            presentInContext.setAction(DataAction.UPDATE);
            repository.save(presentInContext);
            return;
        }
        AuditLogUtil.verifyPresentInContext(presentInContext, newAction);
    }
}
