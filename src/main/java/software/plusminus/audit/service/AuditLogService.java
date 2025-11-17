package software.plusminus.audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.plusminus.audit.exception.AuditException;
import software.plusminus.audit.model.AuditLog;
import software.plusminus.audit.repository.AuditLogRepository;
import software.plusminus.audit.util.AuditLogUtil;
import software.plusminus.context.Context;
import software.plusminus.crud.CrudAction;
import software.plusminus.security.Security;
import software.plusminus.transaction.context.TransactionContext;
import software.plusminus.util.AnnotationUtils;
import software.plusminus.util.EntityUtils;
import software.plusminus.util.FieldUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AuditLogService {

    private final Context<Security> securityContext;
    private final DeviceContext deviceContext;
    private final Context<String> tenantContext;
    private final TransactionIdProvider transactionIdProvider;
    private final AuditLogRepository repository;
    private TransactionContext<List<AuditLog<?>>> currentAuditLogs = TransactionContext.of(ArrayList::new);

    @Transactional(propagation = Propagation.MANDATORY)
    public <T> AuditLog<T> log(T entity, CrudAction action) {
        String entityType = entity.getClass().getName();
        Long entityId = getEntityId(entity, action);
        UUID transactionId = transactionIdProvider.currentTransactionId();
        AuditLog<T> presentInTheContext = findInContext(entityType, entityId, transactionId, action);
        if (presentInTheContext != null) {
            processPresentAuditLog(presentInTheContext, action);
            return presentInTheContext;
        }
        unmarkCurrentAuditLogForEntity(entityType, entityId, action);
        AuditLog<T> auditLog = prepareAuditLog(entity, action, transactionId);
        currentAuditLogs.get().add(auditLog);
        repository.save(auditLog);
        return auditLog;
    }

    private <T> AuditLog<T> prepareAuditLog(T entity, CrudAction action, UUID transactionId) {
        AuditLog<T> auditLog = new AuditLog<>();
        auditLog.setEntity(entity);
        auditLog.setTime(ZonedDateTime.now());
        auditLog.setCurrent(true);
        auditLog.setUsername(securityContext.get().getUsername());
        auditLog.setDevice(deviceContext.currentDevice());
        auditLog.setTransactionId(transactionId);
        if (auditLog.getDevice() == null) {
            auditLog.setDevice("");
        }
        auditLog.setTenant(getTenant(entity));
        auditLog.setAction(action);
        return auditLog;
    }

    private void unmarkCurrentAuditLogForEntity(String entityType, Long entityId, CrudAction action) {
        if (action == CrudAction.CREATE) {
            return;
        }
        repository.findByEntityTypeAndEntityIdAndCurrentTrue(entityType, entityId)
                        .forEach(auditLog -> {
                            auditLog.setCurrent(false);
                            repository.save(auditLog);
                        });
    }

    @Nullable
    private Long getEntityId(Object entity, CrudAction action) {
        Long id = EntityUtils.findId(entity, Long.class);
        if (id == null) {
            if (action == CrudAction.CREATE) {
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
            tenant = tenantContext.get();
        }
        return tenant;
    }

    @Nullable
    private <T> AuditLog<T> findInContext(String entityType, Long entityId, UUID transactionId,
                                          CrudAction action) {
        List<AuditLog<T>> presentInContext = currentAuditLogs.get().stream()
                .filter(auditLog -> {
                    String presentEntityType = auditLog.getEntity().getClass().getName();
                    Long presentEntityId = getEntityId(auditLog.getEntity(), action);
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

    private <T> void processPresentAuditLog(AuditLog<T> presentAuditLog, CrudAction newAction) {
        if (presentAuditLog.getAction() == newAction) {
            return;
        }
        switch (newAction) {
            case READ:
                throw new AuditException("Incorrect action READ on AuditLogService.log()");
            case CREATE:
                AuditLogUtil.verifyPresentAuditLogOnCreate(presentAuditLog);
                break;
            case UPDATE:
                processPresentAuditLogOnUpdate(presentAuditLog);
                break;
            case PATCH:
                AuditLogUtil.verifyPresentAuditLogOnPatch(presentAuditLog);
                break;
            case DELETE:
                processPresentAuditLogOnDelete(presentAuditLog);
                break;
            default:
                throw new AuditException("Unknown combination of AuditLog present in the context (with"
                        + " entity " + presentAuditLog.getEntity()
                        + " and action " + presentAuditLog.getAction()
                        + ") from one side and a new action " + newAction + " from other.");
        }
    }

    private void processPresentAuditLogOnUpdate(AuditLog<?> presentAuditLog) {
        if (presentAuditLog.getAction() == CrudAction.DELETE) {
            presentAuditLog.setAction(CrudAction.UPDATE);
            repository.save(presentAuditLog);
        }
    }

    private void processPresentAuditLogOnDelete(AuditLog<?> presentAuditLog) {
        if (presentAuditLog.getAction() == CrudAction.CREATE) {
            currentAuditLogs.get().remove(presentAuditLog);
            repository.delete(presentAuditLog);
        } else if (presentAuditLog.getAction() == CrudAction.UPDATE) {
            presentAuditLog.setAction(CrudAction.DELETE);
            repository.save(presentAuditLog);
        }
    }
}
