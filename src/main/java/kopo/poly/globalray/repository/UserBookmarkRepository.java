package kopo.poly.globalray.repository;

import kopo.poly.globalray.entity.UserBookmarkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserBookmarkRepository extends JpaRepository<UserBookmarkEntity, Long> {

    List<UserBookmarkEntity> findByUserIdOrderByRegDtDesc(String userId);
    boolean existsByUserIdAndArticleUrl(String userId, String articleUrl);

    Optional<UserBookmarkEntity> findByUserIdAndArticleUrl(String userId, String articleUrl);
    void deleteByUserId(String userId);
}