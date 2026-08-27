package kopo.poly.globalray.repository;

import kopo.poly.globalray.entity.ViewHistoryEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ViewHistoryRepository extends MongoRepository<ViewHistoryEntity, String> {
    List<ViewHistoryEntity> findTop100ByOrderByViewDtDesc();
}
