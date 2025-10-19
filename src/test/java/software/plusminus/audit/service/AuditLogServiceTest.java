package software.plusminus.audit.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.plusminus.audit.fixtures.TestEntity;
import software.plusminus.audit.model.AuditLog;
import software.plusminus.audit.repository.AuditLogRepository;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.context.Context;
import software.plusminus.listener.DataAction;
import software.plusminus.security.context.SecurityContext;
import software.plusminus.tenant.service.TenantService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.plusminus.check.Checks.check;

@RunWith(MockitoJUnitRunner.class)
public class AuditLogServiceTest {

    @Mock
    private Context<List<AuditLog>> auditLogContext;
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

    @InjectMocks
    private AuditLogService service;

    @Captor
    private ArgumentCaptor<AuditLog<TestEntity>> captor;

    private UUID transactionId = UUID.fromString("3a37e67d-a8b2-4c35-9e6f-a4e4b686ffb5");

    @Before
    public void setUp() {
        when(auditLogContext.get()).thenReturn(new ArrayList<>());
        when(securityContext.getUsername()).thenReturn("TestUser");
        when(deviceContext.currentDevice()).thenReturn("TestDevice");
        when(transactionContext.currentTransactionId()).thenReturn(transactionId);
    }

    @Test
    public void log_CreatesAuditLogOnCreate() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        service.log(entity, DataAction.CREATE);

        verify(auditLogRepository).save(captor.capture());
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

        service.log(entity, DataAction.CREATE);

        verify(auditLogRepository).save(captor.capture());
        check(captor.getValue().getTenant()).is(tenant);
    }

    @Test
    public void log_CreatesAuditLogOnUpdate() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        service.log(entity, DataAction.UPDATE);

        verify(auditLogRepository).save(captor.capture());
        check(captor.getValue().getDevice()).is("TestDevice");
        check(captor.getValue().getUsername()).is("TestUser");
        check(captor.getValue().getTenant()).is("Some tenant");
        check(captor.getValue().getTransactionId()).is(transactionId);
    }

    @Test
    public void log_ChangesPreviousAuditLog() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        AuditLog<TestEntity> previous = new AuditLog<>();
        previous.setCurrent(true);
        when(auditLogRepository.<TestEntity>findByEntityTypeAndEntityIdAndCurrentTrue(
                TestEntity.class.getName(), entity.getId()))
                .thenReturn(Collections.singletonList(previous));

        service.log(entity, DataAction.UPDATE);

        check(previous.isCurrent()).is(false);
        verify(auditLogRepository, times(2)).save(captor.capture());
        check(captor.getAllValues().get(0)).is(previous);
    }

    @Test
    public void logDelete_CreatesAuditLogOnDelete() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        service.log(entity, DataAction.DELETE);

        verify(auditLogRepository).save(captor.capture());
        check(captor.getValue().getDevice()).is("TestDevice");
        check(captor.getValue().getUsername()).is("TestUser");
        check(captor.getValue().getTenant()).is("Some tenant");
        check(captor.getValue().getTransactionId()).is(transactionId);
    }
}