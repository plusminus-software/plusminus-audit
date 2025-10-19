package software.plusminus.audit.util;

import lombok.experimental.UtilityClass;
import software.plusminus.audit.exception.AuditException;
import software.plusminus.audit.model.AuditLog;
import software.plusminus.listener.DataAction;

@UtilityClass
public class AuditLogUtil {

    public void verifyPresentInContext(AuditLog<?> presentInContext, DataAction newAction) {
        if (presentInContext.getAction() == DataAction.UPDATE
                && newAction == DataAction.CREATE) {
            throw new AuditException("Cannot save CREATE AuditLog: the UPDATE AuditLog is already present"
                    + " in the context with entity " + presentInContext.getEntity());
        }
        if (presentInContext.getAction() == DataAction.DELETE
                && newAction == DataAction.CREATE) {
            throw new AuditException("Cannot save CREATE AuditLog: the DELETE AuditLog is already present"
                    + " in the context with entity " + presentInContext.getEntity());
        }
        throw new AuditException("Unknown combination of AuditLog present in the context (with"
                + " entity " + presentInContext.getEntity()
                + " and action " + presentInContext.getAction()
                + ") from one side and a new action " + newAction + " from other.");
    }

}
