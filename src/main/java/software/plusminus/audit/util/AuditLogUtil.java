package software.plusminus.audit.util;

import lombok.experimental.UtilityClass;
import software.plusminus.audit.exception.AuditException;
import software.plusminus.audit.model.AuditLog;
import software.plusminus.crud.CrudAction;

@UtilityClass
public class AuditLogUtil {

    public void verifyPresentAuditLogOnCreate(AuditLog<?> presentAuditLog) {
        if (presentAuditLog.getAction() != CrudAction.CREATE) {
            throwException(CrudAction.CREATE, presentAuditLog);
        }
    }

    public void verifyPresentAuditLogOnPatch(AuditLog<?> presentAuditLog) {
        if (presentAuditLog.getAction() == CrudAction.DELETE) {
            throwException(CrudAction.PATCH, presentAuditLog);
        }
    }

    private void throwException(CrudAction newAction, AuditLog<?> presentAuditLog) {
        throw new AuditException("Cannot save " + newAction + " AuditLog: the " + presentAuditLog.getAction()
                + " AuditLog is already present in the transaction for entity " + presentAuditLog.getEntity());
    }
}
