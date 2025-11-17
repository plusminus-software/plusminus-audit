package software.plusminus.audit.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.plusminus.audit.fixtures.TestEntity;
import software.plusminus.audit.fixtures.TransactionalService;
import software.plusminus.audit.model.AuditLog;
import software.plusminus.audit.repository.AuditLogRepository;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.context.Context;
import software.plusminus.crud.CrudAction;
import software.plusminus.data.service.DataService;
import software.plusminus.test.IntegrationTest;

import java.util.UUID;
import javax.persistence.EntityManager;

import static org.mockito.Mockito.when;
import static software.plusminus.check.Checks.check;

public class AuditLogServiceIntegrationTest extends IntegrationTest {

    @MockBean
    private Context<String> tenantContext;
    @MockBean
    private DeviceContext deviceContext;
    @MockBean
    private TransactionIdProvider transactionIdProvider;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private TransactionalService transactionalService;
    @Autowired
    private DataService dataService;
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuditLogService service;

    private UUID transactionId = UUID.fromString("3a37e67d-a8b2-4c35-9e6f-a4e4b686ffb5");

    @Before
    public void before() {
        when(deviceContext.currentDevice()).thenReturn("TestDevice");
        when(transactionIdProvider.currentTransactionId()).thenReturn(transactionId);
        Context.init();
    }

    @After
    public void after() {
        Context.clear();
    }

    @Test
    public void create() {
        checkAuditLog(CrudAction.CREATE);
    }

    @Test
    public void update() {
        checkAuditLog(CrudAction.UPDATE);
    }

    @Test
    public void patch() {
        checkAuditLog(CrudAction.PATCH);
    }

    @Test
    public void delete() {
        checkAuditLog(CrudAction.DELETE);
    }

    @Test
    public void tenant() {
        String tenant = "tenantFromService";
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setTenant(null);
        when(tenantContext.get()).thenReturn(tenant);

        transactionalService.inTransaction(() -> service.log(entity, CrudAction.CREATE));
        AuditLog<?> auditLog = entityManager.find(AuditLog.class, 1L);

        check(auditLog.getTenant()).is(tenant);
    }

    @Test
    public void previousAuditLog() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        transactionalService.inTransaction(() -> service.log(entity, CrudAction.CREATE));
        transactionalService.inTransaction(() -> service.log(entity, CrudAction.UPDATE));

        checkCurrent();
    }

    @Test
    public void nestedTransaction() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        transactionalService.inTransaction(() -> {
            transactionalService.inTransaction(() -> service.log(entity, CrudAction.CREATE));
            service.log(entity, CrudAction.UPDATE);
        });
        AuditLog<?> auditLogFirst = entityManager.find(AuditLog.class, 1L);
        AuditLog<?> auditLogNull = entityManager.find(AuditLog.class, 2L);

        check(auditLogFirst.isCurrent()).is(true);
        check(auditLogFirst.getAction()).is(CrudAction.CREATE);
        check(auditLogNull).isNull();
    }

    @Test
    public void nestedSecondTransaction() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        transactionalService.inTransaction(() -> {
            service.log(entity, CrudAction.CREATE);
            transactionalService.inTransaction(() -> service.log(entity, CrudAction.UPDATE));
        });
        AuditLog<?> auditLogFirst = entityManager.find(AuditLog.class, 1L);
        AuditLog<?> auditLogNull = entityManager.find(AuditLog.class, 2L);

        check(auditLogFirst.isCurrent()).is(true);
        check(auditLogFirst.getAction()).is(CrudAction.CREATE);
        check(auditLogNull).isNull();
    }

    @Test
    public void nestedNewTransaction() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        transactionalService.inTransaction(() -> {
            transactionalService.inNewTransaction(() -> service.log(entity, CrudAction.CREATE));
            service.log(entity, CrudAction.UPDATE);
        });

        checkCurrent();
    }

    @Test
    public void throughDataService() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setId(null);

        TestEntity created = transactionalService.inTransaction(() -> dataService.create(entity));

        transactionalService.inTransaction(() -> {
            created.setMyField("updated1");
            TestEntity updated = dataService.update(created);
            updated.setMyField("updated2");
            dataService.update(updated);
        });

        checkCurrent();
    }

    private void checkAuditLog(CrudAction action) {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        transactionalService.inTransaction(() -> service.log(entity, action));
        AuditLog<?> auditLog = entityManager.find(AuditLog.class, 1L);
        AuditLog<?> auditLogNull = entityManager.find(AuditLog.class, 2L);

        check(auditLog.getAction()).is(action);
        check(auditLog.getDevice()).is("TestDevice");
        check(auditLog.getUsername()).is("TestUser");
        check(auditLog.getTenant()).is("Some tenant");
        check(auditLog.getTransactionId()).is(transactionId);
        check(auditLogNull).isNull();
    }

    private void checkCurrent() {
        AuditLog<?> auditLogFirst = entityManager.find(AuditLog.class, 1L);
        AuditLog<?> auditLogSecond = entityManager.find(AuditLog.class, 2L);

        check(auditLogFirst.isCurrent()).is(false);
        check(auditLogFirst.getAction()).is(CrudAction.CREATE);
        check(auditLogSecond.isCurrent()).is(true);
        check(auditLogSecond.getAction()).is(CrudAction.UPDATE);
    }
}