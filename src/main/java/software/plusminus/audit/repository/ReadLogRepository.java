package software.plusminus.audit.repository;

import org.springframework.data.repository.Repository;
import software.plusminus.audit.model.ReadLog;

import java.util.List;

public interface ReadLogRepository extends Repository<ReadLog, Long> {

    <T> ReadLog<T> save(ReadLog<T> readLog);

    <T> List<ReadLog<T>> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
