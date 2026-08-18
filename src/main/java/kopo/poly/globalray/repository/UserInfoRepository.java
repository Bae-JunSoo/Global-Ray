package kopo.poly.globalray.repository;

import kopo.poly.globalray.entity.UserInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfoEntity, String> {

    Optional<UserInfoEntity> findByUserEmail(String email);
    boolean existsByUserId(String userId);
    boolean existsByUserEmail(String email);
    Optional<UserInfoEntity> findByUserEmailAndUserName(String email, String userName);
}