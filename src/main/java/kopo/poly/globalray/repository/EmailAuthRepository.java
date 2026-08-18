package kopo.poly.globalray.repository;

import kopo.poly.globalray.entity.EmailAuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailAuthRepository extends JpaRepository<EmailAuthEntity, Long> {

    Optional<EmailAuthEntity> findTopByReqEmailOrderByAuthIdDesc(String email);
    void deleteByReqEmail(String email);
}