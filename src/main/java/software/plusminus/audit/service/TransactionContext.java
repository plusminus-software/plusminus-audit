package software.plusminus.audit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.UUID;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

@Component
public class TransactionContext {

    private static final Pattern UUID_REGEX = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Autowired
    private HttpServletRequest request;

    @Nullable
    public UUID currentTransactionId() {
        if (RequestContextHolder.getRequestAttributes() == null) {
            return null;
        }
        String transactionId = request.getParameter("transaction");
        if (transactionId == null || !UUID_REGEX.matcher(transactionId).matches()) {
            return null;
        }
        return UUID.fromString(transactionId);
    }
}
