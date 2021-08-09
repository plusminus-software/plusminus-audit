package software.plusminus.audit.exception;

public class AuditException extends RuntimeException {

    public AuditException() {
    }

    public AuditException(String s) {
        super(s);
    }

    public AuditException(Throwable throwable) {
        super(throwable);
    }
}
