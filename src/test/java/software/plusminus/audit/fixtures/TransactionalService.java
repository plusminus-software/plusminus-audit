package software.plusminus.audit.fixtures;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Callable;

@Service
public class TransactionalService {

    @Transactional
    public void inTransaction(Runnable runnable) {
        runnable.run();
    }

    @Transactional
    public <T> T inTransaction(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void inNewTransaction(Runnable runnable) {
        runnable.run();
    }
}
