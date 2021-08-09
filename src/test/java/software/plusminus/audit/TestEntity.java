package software.plusminus.audit;

import lombok.Data;
import software.plusminus.audit.annotation.Auditable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;

@Data
@Auditable
@Entity
public class TestEntity {

    @Id
    // TODO got error if @GeneratedValue is not IDENTITY on tests - need to fix
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String myField;
    @Version
    private Long version;
    @Tenant
    private String tenant;

}