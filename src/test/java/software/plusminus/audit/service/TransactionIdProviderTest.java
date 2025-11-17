package software.plusminus.audit.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.when;
import static software.plusminus.check.Checks.check;

@RunWith(MockitoJUnitRunner.class)
public class TransactionIdProviderTest {

    @Mock
    private HttpServletRequest request;
    @InjectMocks
    private TransactionIdProvider transactionIdProvider;

    private String transactionId = "3a37e67d-a8b2-4c35-9e6f-a4e4b686ffb5";

    @Before
    public void before() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @After
    public void after() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void correctTransactionId() {
        when(request.getParameter("transaction")).thenReturn(transactionId);
        UUID actual = transactionIdProvider.currentTransactionId();
        check(actual).is(transactionId);
    }

    @Test
    public void incorrectTransactionId() {
        when(request.getParameter("transaction")).thenReturn("incorrect uuid");
        UUID actual = transactionIdProvider.currentTransactionId();
        check(actual).isNull();
    }

    @Test
    public void missedTransactionId() {
        UUID actual = transactionIdProvider.currentTransactionId();
        check(actual).isNull();
    }
}