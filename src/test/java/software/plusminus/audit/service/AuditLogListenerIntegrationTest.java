package software.plusminus.audit.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import software.plusminus.audit.exception.AuditException;
import software.plusminus.audit.fixtures.InnerEntity;
import software.plusminus.audit.fixtures.TestEntity;
import software.plusminus.audit.fixtures.TransactionalService;
import software.plusminus.audit.model.AuditLog;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.crud.CrudAction;
import software.plusminus.data.service.DataService;
import software.plusminus.test.IntegrationTest;

import java.util.UUID;
import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class AuditLogListenerIntegrationTest extends IntegrationTest {

    @Autowired
    private TransactionalService transactionalService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private DataService dataService;

    @MockBean
    private TransactionIdProvider transactionIdProvider;

    @Test
    public void createAndUpdateInDifferentTransactions() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setId(null);
        entity.setVersion(null);
        entity.setTenant("localhost");

        TestEntity created = transactionalService.inTransaction(() -> dataService.create(entity));
        created.setMyField("changed");
        transactionalService.inTransaction(() -> dataService.update(created));

        AuditLog<?> auditLog1 = entityManager.find(AuditLog.class, 1L);
        AuditLog<?> auditLog2 = entityManager.find(AuditLog.class, 2L);
        AuditLog<?> auditLogNull = entityManager.find(AuditLog.class, 3L);
        assertThat(auditLog1.isCurrent()).isFalse();
        assertThat(auditLog2.isCurrent()).isTrue();
        assertThat(auditLogNull).isNull();
    }

    @Test
    public void createAndUpdateInTheSameTransaction() {
        UUID transactionId = UUID.randomUUID();
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setId(null);
        entity.setVersion(null);
        entity.setTenant("localhost");
        when(transactionIdProvider.currentTransactionId())
                .thenReturn(transactionId);

        transactionalService.inTransaction(() -> {
            TestEntity created = dataService.create(entity);
            dataService.update(created);
        });

        AuditLog<?> auditLog1 = entityManager.find(AuditLog.class, 1L);
        AuditLog<?> auditLogNull = entityManager.find(AuditLog.class, 2L);
        assertThat(auditLog1.isCurrent()).isTrue();
        assertThat(auditLog1.getTransactionId()).isEqualTo(transactionId);
        assertThat(auditLog1.getAction()).isEqualTo(CrudAction.CREATE);
        assertThat(auditLogNull).isNull();
    }

    @Test
    public void createAndDeleteInTheSameTransaction() {
        UUID transactionId = UUID.randomUUID();
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setId(null);
        entity.setVersion(null);
        entity.setTenant("localhost");
        when(transactionIdProvider.currentTransactionId())
                .thenReturn(transactionId);

        transactionalService.inTransaction(() -> {
            TestEntity created = dataService.create(entity);
            dataService.delete(created);
        });

        AuditLog<?> auditLogNull = entityManager.find(AuditLog.class, 1L);
        assertThat(auditLogNull).isNull();
    }

    @Test
    public void createWithInnerEntityInTheSameTransaction() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setId(null);
        entity.setVersion(null);
        entity.setTenant("localhost");
        InnerEntity innerEntity = new InnerEntity();
        innerEntity.setId(null);
        innerEntity.setVersion(null);
        innerEntity.setTenant("localhost");
        entity.setInnerEntity(innerEntity);
        transactionalService.inTransaction(() -> {
            dataService.create(entity);
            dataService.create(innerEntity);
        });
        AuditLog<?> auditLog1 = entityManager.find(AuditLog.class, 1L);
        AuditLog<?> auditLog2 = entityManager.find(AuditLog.class, 2L);
        AuditLog<?> auditLogNull = entityManager.find(AuditLog.class, 3L);
        assertThat(auditLog1.isCurrent()).isTrue();
        assertThat(auditLog2.isCurrent()).isTrue();
        assertThat(auditLogNull).isNull();
    }

    @Test
    public void exceptionOnCreate() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setId(null);
        entity.setVersion(null);
        entity.setTenant("localhost");

        boolean exception = false;
        try {
            transactionalService.inTransaction(() -> {
                dataService.create(entity);
                throw new AuditException("Test exception");
            });
        } catch (IllegalStateException ignored) {
            exception = true;
        }

        AuditLog<?> auditLog1 = entityManager.find(AuditLog.class, 1L);
        assertThat(auditLog1).isNull();
        assertThat(exception).isTrue();
    }

    @Test
    public void exceptionOnUpdate() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setId(null);
        entity.setVersion(null);
        entity.setTenant("localhost");
        TestEntity created = transactionalService.inTransaction(() -> dataService.create(entity));
        boolean exception = false;
        try {
            transactionalService.inTransaction(() -> {
                created.setMyField("changed");
                created.setVersion(123L);
                dataService.update(created);
            });
        } catch (ObjectOptimisticLockingFailureException ignored) {
            exception = true;
        }

        AuditLog<?> auditLog1 = entityManager.find(AuditLog.class, 1L);
        AuditLog<?> auditLog2 = entityManager.find(AuditLog.class, 2L);
        assertThat(auditLog1.isCurrent()).isTrue();
        assertThat(auditLog2).isNull();
        assertThat(exception).isTrue();
    }

    @Test
    public void versionOnCreateAndUpdateInDifferentTransactions() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setId(null);
        entity.setVersion(null);
        entity.setTenant("localhost");

        TestEntity created = transactionalService.inTransaction(() -> dataService.create(entity));
        created.setMyField("changed 1");
        TestEntity updated1 = transactionalService.inTransaction(() -> dataService.update(created));
        updated1.setMyField("changed 2");
        TestEntity updated2 = transactionalService.inTransaction(() -> dataService.update(updated1));

        assertThat(entity.getVersion()).isZero();
        assertThat(created.getVersion()).isZero();
        assertThat(updated1.getVersion()).isEqualTo(1);
        assertThat(updated2.getVersion()).isEqualTo(2);
        assertThat(entityManager.find(TestEntity.class, 1L).getVersion()).isEqualTo(2);
    }

    @Test
    public void versionOnCreateAndUpdateInSameTransaction() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setId(null);
        entity.setVersion(null);
        entity.setTenant("localhost");
        transactionalService.inTransaction(() -> {
            TestEntity created = dataService.create(entity);
            created.setMyField("1");
            TestEntity updated1 = dataService.update(created);
            updated1.setMyField("2");
            dataService.update(updated1);
        });

        assertThat(entity.getVersion()).isEqualTo(1);
        assertThat(entityManager.find(TestEntity.class, 1L).getVersion()).isEqualTo(1);
    }
}