package software.plusminus.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import software.plusminus.audit.model.WriteLog;

import java.util.List;

public interface WriteLogRepository extends Repository<WriteLog, Long> {

    <T> WriteLog<T> save(WriteLog<T> writeLog);

    <T> WriteLog<T> findByEntityTypeAndEntityIdAndCurrentTrue(String entityType, Long entityId);

    @SuppressWarnings("squid:S1452")
    <T> Page<WriteLog<? extends T>> findByEntityTypeInAndDeviceIsNotAndNumberGreaterThanAndCurrentTrue(
            List<String> types, String ignoreDevice, Long numberGreaterThan, Pageable pageable);

}
