package kopo.poly.globalray.repository;

import kopo.poly.globalray.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, String> {

    Optional<UserSessionEntity> findByUserId(String userId);
    void deleteByUserId(String userId);
}