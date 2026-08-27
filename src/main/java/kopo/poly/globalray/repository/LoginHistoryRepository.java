package kopo.poly.globalray.repository;

import kopo.poly.globalray.entity.LoginHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistoryEntity, Long> {
    List<LoginHistoryEntity> findTop100ByOrderByLoginDtDesc();
}
