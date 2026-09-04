package kopo.poly.globalray.repository;

import kopo.poly.globalray.entity.UserLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserLikeRepository extends JpaRepository<UserLikeEntity, Long> {

    List<UserLikeEntity> findByUserId(String userId);
    Optional<UserLikeEntity> findByUserIdAndArticleUrl(String userId, String articleUrl);
    boolean existsByUserIdAndArticleUrl(String userId, String articleUrl);
    void deleteByUserId(String userId);
}
