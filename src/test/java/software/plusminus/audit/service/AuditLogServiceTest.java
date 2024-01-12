package software.plusminus.audit.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import software.plusminus.audit.TestEntity;
import software.plusminus.audit.model.AuditLog;
import software.plusminus.audit.repository.AuditLogRepository;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.security.context.SecurityContext;
import software.plusminus.tenant.service.TenantService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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

    @InjectMocks
    private AuditLogService service;

    @Captor
    private ArgumentCaptor<AuditLog> captor;

    private UUID transactionId = UUID.fromString("3a37e67d-a8b2-4c35-9e6f-a4e4b686ffb5");

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(service, "self", service);
        when(securityContext.getUsername()).thenReturn("TestUser");
        when(deviceContext.currentDevice()).thenReturn("TestDevice");
        when(transactionContext.currentTransactionId()).thenReturn(transactionId);
    }

    @Test
    public void logCreate_CreatesAuditLog() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        service.logCreate(entity);

        verify(auditLogRepository).save(captor.capture());
        check(captor.getValue().getDevice()).is("TestDevice");
        check(captor.getValue().getUsername()).is("TestUser");
        check(captor.getValue().getTenant()).is("Some tenant");
        check(captor.getValue().getTransactionId()).is(transactionId);
    }
    
    @Test
    public void logCreate_PopulatesTenant() {
        String tenant = "tenantFromService";
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        entity.setTenant(null);
        when(tenantService.currentTenant()).thenReturn(tenant);

        service.logCreate(entity);

        verify(auditLogRepository).save(captor.capture());
        check(captor.getValue().getTenant()).is(tenant);
    }

    @Test
    public void logUpdate_CreatesAuditLog() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        when(auditLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        prepareCurrentAuditLog();

        service.logUpdate(entity);

        verify(auditLogRepository, times(2)).save(captor.capture());
        check(captor.getAllValues().get(1).getDevice()).is("TestDevice");
        check(captor.getAllValues().get(1).getUsername()).is("TestUser");
        check(captor.getValue().getTenant()).is("Some tenant");
        check(captor.getValue().getTransactionId()).is(transactionId);
    }

    @Test
    public void logUpdate_ChangesPreviousAuditLog() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        when(auditLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        AuditLog previousAuditLog = prepareCurrentAuditLog();

        service.logUpdate(entity);

        check(previousAuditLog.isCurrent()).isFalse();
        verify(auditLogRepository, times(2)).save(captor.capture());
        check(captor.getAllValues().get(0)).is(previousAuditLog);
    }

    @Test
    public void logDelete_CreatesAuditLog() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        when(auditLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        prepareCurrentAuditLog();

        service.logDelete(entity);

        verify(auditLogRepository, times(2)).save(captor.capture());
        check(captor.getAllValues().get(1).getDevice()).is("TestDevice");
        check(captor.getAllValues().get(1).getUsername()).is("TestUser");
        check(captor.getAllValues().get(1).getTenant()).is("Some tenant");
        check(captor.getAllValues().get(1).getTransactionId()).is(transactionId);
    }

    @Test
    public void logDelete_ChangesPreviousAuditLog() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        when(auditLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        AuditLog previousAuditLog = prepareCurrentAuditLog();

        service.logDelete(entity);

        check(previousAuditLog.isCurrent()).isFalse();
        verify(auditLogRepository, times(2)).save(captor.capture());
        check(captor.getAllValues().get(0)).is(previousAuditLog);
    }

    @Test
    public void unmarkCurrentAuditLog() {
        AuditLog current = prepareCurrentAuditLog();
        when(auditLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        AuditLog result = service.unmarkCurrentAuditLog(TestEntity.class.getName(), 1L);

        check(result).isNotNull();
        check(result.getNumber()).is(current.getNumber());
        check(result.isCurrent()).isFalse();
        verify(auditLogRepository).save(captor.capture());
        check(captor.getValue()).isSame(result);
    }

    private AuditLog prepareCurrentAuditLog() {
        AuditLog previousAuditLog = new AuditLog();
        previousAuditLog.setNumber(4L);
        previousAuditLog.setCurrent(true);
        when(auditLogRepository.findByEntityTypeAndEntityIdAndCurrentTrue(TestEntity.class.getName(), 1L))
                .thenReturn(previousAuditLog);
        return previousAuditLog;
    }
}