package software.plusminus.audit.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;

@Data
@EqualsAndHashCode(of = "", callSuper = true)
@ToString(of = "", callSuper = true)
@Entity
public class ReadLog<T> extends DataLog<T> {

    @Column(updatable = false)
    private Integer version;

}
