package software.plusminus.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import software.plusminus.audit.model.AuditLog;

import java.util.List;

public interface AuditLogRepository extends Repository<AuditLog<?>, Long> {

    default <T> List<AuditLog<T>> findByEntityTypeAndEntityIdAndCurrentTrue(Class<T> entityType, Long entityId) {
        return findByEntityTypeAndEntityIdAndCurrentTrue(entityType.getName(), entityId);
    }

    <T> List<AuditLog<T>> findByEntityTypeAndEntityIdAndCurrentTrue(String entityType, Long entityId);

    @SuppressWarnings("squid:S1452")
    <T> Page<AuditLog<? extends T>> findByEntityTypeInAndNumberGreaterThanAndCurrentTrue(
            List<String> types, Long numberGreaterThan, Pageable pageable);
    
    @SuppressWarnings("squid:S1452")
    <T> Page<AuditLog<? extends T>> findByEntityTypeInAndDeviceIsNotAndNumberGreaterThanAndCurrentTrue(
            List<String> types, String ignoreDevice, Long numberGreaterThan, Pageable pageable);

    @SuppressWarnings("squid:S1452")
    <T> Page<AuditLog<? extends T>> findByCurrentTrue(Pageable pageable);

    <T> AuditLog<T> save(AuditLog<T> auditLog);

    <T> AuditLog<T> delete(AuditLog<T> auditLog);

}
