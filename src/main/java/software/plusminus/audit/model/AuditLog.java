package software.plusminus.audit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

@Data
@EqualsAndHashCode(of = "number")
@ToString(of = "number")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenant", type = "string"))
@Filter(name = "tenantFilter", condition = "tenant = :tenant")
@Entity
public class AuditLog<T> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long number;

    private String tenant;

    @Any(
            metaColumn = @Column(name = "entity_type"),
            fetch = FetchType.LAZY)
    @AnyMetaDef(
            idType = "long",
            metaType = "string",
            metaValues = {})
    @JoinColumn(name = "entity_id")
    @JsonIgnore
    private T entity;

    @Column(name = "entity_type", insertable = false, updatable = false, nullable = false)
    private String entityType;

    @Column(name = "entity_id", insertable = false, updatable = false, nullable = false)
    private Long entityId;

    @Column(updatable = false)
    private ZonedDateTime time;

    @Column(updatable = false)
    private String username;

    @Column(updatable = false)
    private String device;

    @Enumerated(EnumType.STRING)
    private DataAction action;

    private boolean current;

}
