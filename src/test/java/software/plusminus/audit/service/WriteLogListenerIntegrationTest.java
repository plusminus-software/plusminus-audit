package software.plusminus.audit.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import software.plusminus.audit.TestEntity;
import software.plusminus.audit.TestEntityRepository;
import software.plusminus.audit.TransactionalService;
import software.plusminus.audit.exception.AuditException;
import software.plusminus.audit.model.WriteLog;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.security.context.SecurityContext;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class WriteLogListenerIntegrationTest {

    @Autowired
    private TransactionalService transactionalService;
    @Autowired
    private TestEntityRepository entityRepository;
    @Autowired
    private EntityManager entityManager;

    @SuppressWarnings("PMD.UnusedPrivateField")
    @MockBean
    private SecurityContext securityContext;

    @Test
    public void createAndUpdateInDifferentTransactions() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setId(null);
        entity.setVersion(null);
        entity.setTenant("localhost");

        TestEntity created = entityRepository.save(entity);
        created.setMyField("changed");
        entityRepository.save(created);

        WriteLog writeLog1 = entityManager.find(WriteLog.class, 1L);
        WriteLog writeLog2 = entityManager.find(WriteLog.class, 2L);
        WriteLog writeLogNull = entityManager.find(WriteLog.class, 3L);
        assertThat(writeLog1.isCurrent()).isFalse();
        assertThat(writeLog2.isCurrent()).isTrue();
        assertThat(writeLogNull).isNull();
    }

    @Test
    public void createAndUpdateInTheSameTransaction() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setId(null);
        entity.setVersion(null);
        entity.setTenant("localhost");
        transactionalService.inTransaction(() -> {
            TestEntity created = entityRepository.save(entity);
            created.setMyField("changed");
            entityRepository.save(created);
        });
        WriteLog writeLog1 = entityManager.find(WriteLog.class, 1L);
        WriteLog writeLogNull = entityManager.find(WriteLog.class, 2L);
        assertThat(writeLog1.isCurrent()).isTrue();
        assertThat(writeLogNull).isNull();
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
                entityRepository.save(entity);
                throw new AuditException("Test exception");
            });
        } catch (AuditException ignored) {
            exception = true;
        }

        WriteLog writeLog1 = entityManager.find(WriteLog.class, 1L);
        assertThat(writeLog1).isNull();
        assertThat(exception).isTrue();
    }

    @Test
    public void exceptionOnUpdate() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setId(null);
        entity.setVersion(null);
        entity.setTenant("localhost");
        TestEntity created = entityRepository.save(entity);
        boolean exception = false;
        try {
            transactionalService.inTransaction(() -> {
                created.setMyField("changed");
                created.setVersion(123L);
                entityRepository.save(created);
            });
        } catch (ObjectOptimisticLockingFailureException ignored) {
            exception = true;
        }

        WriteLog writeLog1 = entityManager.find(WriteLog.class, 1L);
        WriteLog writeLog2 = entityManager.find(WriteLog.class, 2L);
        assertThat(writeLog1.isCurrent()).isTrue();
        assertThat(writeLog2).isNull();
        assertThat(exception).isTrue();
    }

    @Test
    public void versionOnCreateAndUpdateInDifferentTransactions() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setId(null);
        entity.setVersion(null);
        entity.setTenant("localhost");

        TestEntity created = entityRepository.save(entity);
        created.setMyField("changed 1");
        TestEntity updated1 = entityRepository.save(created);
        updated1.setMyField("changed 2");
        TestEntity updated2 = entityRepository.save(updated1);

        assertThat(entity.getVersion()).isEqualTo(0);
        assertThat(created.getVersion()).isEqualTo(0);
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
            TestEntity created = entityRepository.save(entity);
            created.setMyField("1");
            TestEntity updated1 = entityRepository.save(created);
            updated1.setMyField("2");
            entityRepository.save(updated1);
        });

        assertThat(entity.getVersion()).isEqualTo(1);
        assertThat(entityManager.find(TestEntity.class, 1L).getVersion()).isEqualTo(1);
    }
}