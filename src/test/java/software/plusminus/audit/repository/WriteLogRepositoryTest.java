package software.plusminus.audit.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import software.plusminus.audit.TestEntity;
import software.plusminus.audit.model.WriteLog;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.security.context.SecurityContext;

import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static software.plusminus.check.Checks.check;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class WriteLogRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private WriteLogRepository repository;

    @SuppressWarnings("PMD.UnusedPrivateField")
    @MockBean
    private SecurityContext securityService;

    private List<TestEntity> entities;
    private List<WriteLog> writeLogs;

    @Before
    public void before() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        entities = JsonUtils.fromJsonList("/json/test-entities.json", TestEntity[].class);
        writeLogs = JsonUtils.fromJsonList("/json/writelogs.json", WriteLog[].class);
        entities.forEach(this::prepareEntityAndCommits);

        entities.forEach(entityManager::persist);
        entityManager.createQuery("delete from WriteLog").executeUpdate();
        writeLogs.forEach(entityManager::persist);
    }

    @Transactional
    @Test
    public void findByEntityTypeAndEntityId() {
        WriteLog result = repository.findByEntityTypeAndEntityIdAndCurrentTrue(
                TestEntity.class.getName(), 2L);
        check(result).is(writeLogs.get(5));
    }

    @Transactional
    @Test
    public void findIgnoringDevice() {
        List<WriteLog<?>> result = repository.findByEntityTypeInAndDeviceIsNotAndNumberGreaterThanAndCurrentTrue(
                Collections.singletonList(TestEntity.class.getName()),
                "Device 2",
                3L,
                Pageable.unpaged())
                .getContent();

        check(result).hasSize(2);
        check(result.get(0)).is(writeLogs.get(2));
        check(result.get(1)).is(writeLogs.get(8));
    }

    private void prepareEntityAndCommits(TestEntity entity) {
        int index = entities.indexOf(entity);
        writeLogs.subList(index * 3, index * 3 + 3)
                .forEach(writeLog -> {
                    writeLog.setNumber(null);
                    writeLog.setEntityType(null);
                    writeLog.setEntityId(null);
                    writeLog.setEntity(entity);
                });

        entity.setId(null);
    }
}