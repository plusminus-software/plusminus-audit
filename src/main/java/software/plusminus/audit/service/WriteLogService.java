package software.plusminus.audit.service;

import company.plusminus.util.AnnotationUtils;
import company.plusminus.util.EntityUtils;
import company.plusminus.util.FieldUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.plusminus.audit.exception.AuditException;
import software.plusminus.audit.model.WriteAction;
import software.plusminus.audit.model.WriteLog;
import software.plusminus.audit.repository.WriteLogRepository;
import software.plusminus.security.context.DeviceContext;
import software.plusminus.security.context.SecurityContext;

import java.time.ZonedDateTime;
import javax.annotation.Nullable;

@Service
public class WriteLogService {

    @Autowired
    private SecurityContext securityContext;
    @Autowired
    private DeviceContext deviceContext;
    @Autowired
    private WriteLogService self;
    @Autowired
    private WriteLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public <T> WriteLog<T> logCreate(T entity) {
        WriteLog<T> writeLog = prepareWrite(entity, WriteAction.CREATE);
        return repository.save(writeLog);
    }

    @Nullable
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> WriteLog<T> logUpdate(T entity) {
        boolean updated = unmarkCurrentWriteLogForEntity(entity);
        if (!updated) {
            return null;
        }
        WriteLog<T> writeLog = prepareWrite(entity, WriteAction.UPDATE);
        return repository.save(writeLog);
    }

    @Nullable
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> WriteLog<T> logDelete(T entity) {
        boolean updated = unmarkCurrentWriteLogForEntity(entity);
        if (!updated) {
            return null;
        }
        WriteLog<T> writeLog = prepareWrite(entity, WriteAction.DELETE);
        return repository.save(writeLog);
    }

    @Nullable
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> WriteLog<T> unmarkCurrentWriteLog(String entityType, Long entityId) {
        WriteLog<T> previousWriteLog = repository.findByEntityTypeAndEntityIdAndCurrentTrue(entityType, entityId);
        if (previousWriteLog == null) {
            return null;
        }
        previousWriteLog.setCurrent(false);
        return repository.save(previousWriteLog);
    }

    private <T> WriteLog<T> prepareWrite(T entity, WriteAction action) {
        WriteLog<T> writeLog = new WriteLog<>();
        writeLog.setEntity(entity);
        writeLog.setTime(ZonedDateTime.now());
        writeLog.setCurrent(true);
        writeLog.setUsername(securityContext.getUsername());
        writeLog.setDevice(deviceContext.currentDevice());
        if (writeLog.getDevice() == null) {
            writeLog.setDevice(StringUtils.EMPTY);
        }
        writeLog.setTenant(FieldUtils.readFirst(entity, String.class,
                field -> AnnotationUtils.isArrayContain(field.getAnnotations(), "Tenant")));
        writeLog.setAction(action);
        return writeLog;
    }

    private <T> boolean unmarkCurrentWriteLogForEntity(T entity) {
        String entityType = entity.getClass().getName();
        Long entityId = getEntityId(entity);
        WriteLog<T> previousWriteLog = self.unmarkCurrentWriteLog(entityType, entityId);
        return previousWriteLog != null;
    }

    private Long getEntityId(Object entity) {
        Long id = EntityUtils.findId(entity, Long.class);
        if (id == null) {
            throw new AuditException("Can't save WriteLog: entity id is null");
        }
        return id;
    }
}
