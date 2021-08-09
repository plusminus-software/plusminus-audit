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
import software.plusminus.audit.model.WriteLog;
import software.plusminus.audit.repository.WriteLogRepository;
import software.plusminus.check.util.JsonUtils;
import software.plusminus.security.context.DeviceContext;
import software.plusminus.security.context.SecurityContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.plusminus.check.Checks.check;

@RunWith(MockitoJUnitRunner.class)
public class WriteLogServiceTest {

    @Mock
    private SecurityContext securityContext;
    @Mock
    private DeviceContext deviceContext;
    @Mock
    private WriteLogRepository writeLogRepository;

    @InjectMocks
    private WriteLogService service;

    @Captor
    private ArgumentCaptor<WriteLog> captor;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(service, "self", service);
        when(securityContext.getUsername()).thenReturn("TestUser");
        when(deviceContext.currentDevice()).thenReturn("TestDevice");
    }

    @Test
    public void logCreate_CreatesWriteLog() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);

        service.logCreate(entity);

        verify(writeLogRepository).save(captor.capture());
        check(captor.getValue().getDevice()).is("TestDevice");
        check(captor.getValue().getUsername()).is("TestUser");
        check(captor.getValue().getTenant()).is("Some tenant");
    }

    @Test
    public void logUpdate_CreatesWriteLog() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        when(writeLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        prepareCurrentWriteLog();

        service.logUpdate(entity);

        verify(writeLogRepository, times(2)).save(captor.capture());
        check(captor.getAllValues().get(1).getDevice()).is("TestDevice");
        check(captor.getAllValues().get(1).getUsername()).is("TestUser");
        check(captor.getValue().getTenant()).is("Some tenant");
    }

    @Test
    public void logUpdate_ChangesPreviousWriteLog() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        when(writeLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        WriteLog previousWriteLog = prepareCurrentWriteLog();

        service.logUpdate(entity);

        check(previousWriteLog.isCurrent()).isFalse();
        verify(writeLogRepository, times(2)).save(captor.capture());
        check(captor.getAllValues().get(0)).is(previousWriteLog);
    }

    @Test
    public void logDelete_CreatesWriteLog() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        when(writeLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        prepareCurrentWriteLog();

        service.logDelete(entity);

        verify(writeLogRepository, times(2)).save(captor.capture());
        check(captor.getAllValues().get(1).getDevice()).is("TestDevice");
        check(captor.getAllValues().get(1).getUsername()).is("TestUser");
        check(captor.getAllValues().get(1).getTenant()).is("Some tenant");
    }

    @Test
    public void logDelete_ChangesPreviousWriteLog() {
        TestEntity entity = JsonUtils.fromJson("/json/test-entity.json", TestEntity.class);
        when(writeLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
        WriteLog previousWriteLog = prepareCurrentWriteLog();

        service.logDelete(entity);

        check(previousWriteLog.isCurrent()).isFalse();
        verify(writeLogRepository, times(2)).save(captor.capture());
        check(captor.getAllValues().get(0)).is(previousWriteLog);
    }

    @Test
    public void unmarkCurrentWriteLog() {
        WriteLog current = prepareCurrentWriteLog();
        when(writeLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        WriteLog result = service.unmarkCurrentWriteLog(TestEntity.class.getName(), 1L);

        check(result).isNotNull();
        check(result.getNumber()).is(current.getNumber());
        check(result.isCurrent()).isFalse();
        verify(writeLogRepository).save(captor.capture());
        check(captor.getValue()).isSame(result);
    }

    private WriteLog prepareCurrentWriteLog() {
        WriteLog previousWriteLog = new WriteLog();
        previousWriteLog.setNumber(4L);
        previousWriteLog.setCurrent(true);
        when(writeLogRepository.findByEntityTypeAndEntityIdAndCurrentTrue(TestEntity.class.getName(), 1L))
                .thenReturn(previousWriteLog);
        return previousWriteLog;
    }
}