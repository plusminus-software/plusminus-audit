package software.plusminus.audit.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Data
@EqualsAndHashCode(of = "", callSuper = true)
@ToString(of = "", callSuper = true)
@Entity
public class WriteLog<T> extends DataLog<T> {

    @Enumerated(EnumType.STRING)
    private WriteAction action;

    private boolean current;

}
