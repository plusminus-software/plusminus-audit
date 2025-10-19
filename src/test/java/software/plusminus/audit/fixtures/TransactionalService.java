package software.plusminus.audit.fixtures;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.plusminus.context.Context;

import java.util.function.Supplier;

@Service
public class TransactionalService {

    @Transactional
    public void inTransaction(Runnable runnable) {
        Context.init();
        runnable.run();
        Context.clear();
    }

    @Transactional
    public <T> T inTransaction(Supplier<T> supplier) {
        Context.init();
        T result = supplier.get();
        Context.clear();
        return result;
    }

}
