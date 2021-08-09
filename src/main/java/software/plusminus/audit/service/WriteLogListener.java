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

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;

@Component
@ConditionalOnBean(WriteLogService.class)
@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
public class WriteLogListener implements PreInsertEventListener,
        PreUpdateEventListener, PreDeleteEventListener {

    @Autowired
    private transient EntityManagerFactory entityManagerFactory;
    @Autowired
    private transient WriteLogService service;

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
        if (isLoggable(event)) {
            service.logCreate(event.getEntity());
        }
        return false;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        if (isLoggable(event)) {
            service.logUpdate(event.getEntity());
        }
        return false;
    }

    @Override
    public boolean onPreDelete(PreDeleteEvent event) {
        if (isLoggable(event)) {
            service.logDelete(event.getEntity());
        }
        return false;
    }

    private boolean isLoggable(AbstractPreDatabaseOperationEvent event) {
        return AnnotationUtils.findAnnotation(event.getEntity().getClass(), Auditable.class) != null;
    }
}
