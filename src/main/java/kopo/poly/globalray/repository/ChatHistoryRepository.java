package kopo.poly.globalray.repository;

import kopo.poly.globalray.entity.ChatHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistoryEntity, Long> {

    List<ChatHistoryEntity> findTop20ByUserIdOrderByRegDtDesc(String userId);

    void deleteByUserId(String userId);
}
