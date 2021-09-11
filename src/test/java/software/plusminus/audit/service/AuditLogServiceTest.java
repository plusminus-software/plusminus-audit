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
import software.plusminus.security.context.DeviceContext;
import software.plusminus.security.context.SecurityContext;

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
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService service;

    @Captor
    private ArgumentCaptor<AuditLog> captor;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(service, "self", service);
        when(securityContext.getUsername()).thenReturn("TestUser");
        when(deviceContext.currentDevice()).thenReturn("TestDevice");
    }

    @Test
    public void logCreate_CreatesAuditLog() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        service.logCreate(entity);

        verify(auditLogRepository).save(captor.capture());
        check(captor.getValue().getDevice()).is("TestDevice");
        check(captor.getValue().getUsername()).is("TestUser");
        check(captor.getValue().getTenant()).is("Some tenant");
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