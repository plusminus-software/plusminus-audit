package software.plusminus.audit;

import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

public interface TestEntityRepository extends Repository<TestEntity, Long> {

    @Transactional
    TestEntity save(TestEntity testEntity);

}
