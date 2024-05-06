package software.plusminus.audit.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.plusminus.audit.TestEntity;
import software.plusminus.audit.model.AuditLog;
import software.plusminus.audit.model.DataAction;
import software.plusminus.audit.repository.AuditLogRepository;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.security.context.SecurityContext;
import software.plusminus.tenant.service.TenantService;

import java.util.UUID;
import javax.persistence.EntityManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.plusminus.check.Checks.check;

@RunWith(MockitoJUnitRunner.class)
public class AuditLogServiceTest {

    @Mock
    private SecurityContext securityContext;
    @Mock
    private DeviceContext deviceContext;
    @Mock
    private TransactionContext transactionContext;
    @Mock
    private TenantService tenantService;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock(answer = Answers.RETURNS_MOCKS)
    private EntityManager session;

    @InjectMocks
    private AuditLogService service;

    @Captor
    private ArgumentCaptor<AuditLog> captor;

    private UUID transactionId = UUID.fromString("3a37e67d-a8b2-4c35-9e6f-a4e4b686ffb5");

    @Before
    public void setUp() {
        when(securityContext.getUsername()).thenReturn("TestUser");
        when(deviceContext.currentDevice()).thenReturn("TestDevice");
        when(transactionContext.currentTransactionId()).thenReturn(transactionId);
    }

    @Test
    public void log_CreatesAuditLogOnCreate() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        service.log(session, entity, DataAction.CREATE);

        verify(session).persist(captor.capture());
        check(captor.getValue().getDevice()).is("TestDevice");
        check(captor.getValue().getUsername()).is("TestUser");
        check(captor.getValue().getTenant()).is("Some tenant");
        check(captor.getValue().getTransactionId()).is(transactionId);
    }
    
    @Test
    public void log_PopulatesTenant() {
        String tenant = "tenantFromService";
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setTenant(null);
        when(tenantService.currentTenant()).thenReturn(tenant);

        service.log(session, entity, DataAction.CREATE);

        verify(session).persist(captor.capture());
        check(captor.getValue().getTenant()).is(tenant);
    }

    @Test
    public void log_CreatesAuditLogOnUpdate() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        service.log(session, entity, DataAction.UPDATE);

        verify(session).persist(captor.capture());
        check(captor.getValue().getDevice()).is("TestDevice");
        check(captor.getValue().getUsername()).is("TestUser");
        check(captor.getValue().getTenant()).is("Some tenant");
        check(captor.getValue().getTransactionId()).is(transactionId);
    }

    @Test
    public void log_ChangesPreviousAuditLog() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        service.log(session, entity, DataAction.UPDATE);

        verify(session).createQuery(
                "update AuditLog a set a.current = false where entityType = ?1 and entityId = ?2");
    }

    @Test
    public void logDelete_CreatesAuditLogOnDelete() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        service.log(session, entity, DataAction.DELETE);

        verify(session).persist(captor.capture());
        check(captor.getValue().getDevice()).is("TestDevice");
        check(captor.getValue().getUsername()).is("TestUser");
        check(captor.getValue().getTenant()).is("Some tenant");
        check(captor.getValue().getTransactionId()).is(transactionId);
    }

    @Test
    public void log_IgnoresIfThereIsAuditLogWithSameTransactionId() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        AuditLog<TestEntity> previous = prepareCurrentAuditLog();

        AuditLog<TestEntity> result = service.log(session, entity, DataAction.UPDATE);

        check(result).isSame(previous);
        verify(session).createQuery("update AuditLog a set a.action = ?1 where number = ?2");
        verify(session, never()).persist(any());
    }

    private AuditLog<TestEntity> prepareCurrentAuditLog() {
        AuditLog<TestEntity> previousAuditLog = new AuditLog<>();
        previousAuditLog.setNumber(4L);
        when(auditLogRepository.<TestEntity>findByEntityTypeAndEntityIdAndTransactionId(
                TestEntity.class.getName(), 1L, transactionId))
                .thenReturn(previousAuditLog);
        return previousAuditLog;
    }

}