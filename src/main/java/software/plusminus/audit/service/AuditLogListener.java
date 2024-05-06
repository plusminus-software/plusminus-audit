package software.plusminus.audit.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AbstractPreDatabaseOperationEvent;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import software.plusminus.audit.annotation.Auditable;
import software.plusminus.audit.model.DataAction;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;

@Component
@ConditionalOnBean(AuditLogService.class)
@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
public class AuditLogListener implements PreInsertEventListener,
        PreUpdateEventListener, PreDeleteEventListener {

    @Autowired
    private transient EntityManagerFactory entityManagerFactory;
    @Autowired
    private transient AuditLogService service;

    @PostConstruct
    private void init() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.PRE_INSERT).appendListener(this);
        registry.getEventListenerGroup(EventType.PRE_UPDATE).appendListener(this);
        registry.getEventListenerGroup(EventType.PRE_DELETE).appendListener(this);
    }

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        log(event, DataAction.CREATE);
        return false;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        log(event, DataAction.UPDATE);
        return false;
    }

    @Override
    public boolean onPreDelete(PreDeleteEvent event) {
        log(event, DataAction.DELETE);
        return false;
    }

    private void log(AbstractPreDatabaseOperationEvent event, DataAction action) {
        if (!isLoggable(event)) {
            return;
        }
        event.getSession().getActionQueue()
                .registerProcess(session -> service.log(session, event.getEntity(), action));
    }

    private boolean isLoggable(AbstractPreDatabaseOperationEvent event) {
        return AnnotationUtils.findAnnotation(event.getEntity().getClass(), Auditable.class) != null;
    }
}
