package software.plusminus.audit.service;

import lombok.AllArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.UUID;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

@AllArgsConstructor
@Component
public class TransactionIdProvider {

    private static final Pattern UUID_REGEX = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

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
