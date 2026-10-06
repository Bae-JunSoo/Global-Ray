package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.entity.UserLikeEntity;
import kopo.poly.globalray.repository.NewsArticleRepository;
import kopo.poly.globalray.repository.UserLikeRepository;
import kopo.poly.globalray.service.ILikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements ILikeService {

    private final UserLikeRepository userLikeRepository;
    private final NewsArticleRepository newsArticleRepository;

    @Override
    @Transactional
    public boolean toggleLike(String userId, String articleUrl) {
        var existing = userLikeRepository.findByUserIdAndArticleUrl(userId, articleUrl);

        if (existing.isPresent()) {
            userLikeRepository.delete(existing.get());
            newsArticleRepository.decreaseLikeCount(articleUrl);
            return false;
        }

        userLikeRepository.save(UserLikeEntity.builder()
                .userId(userId)
                .articleUrl(articleUrl)
                .build());
        newsArticleRepository.increaseLikeCount(articleUrl);
        return true;
    }

    // 좋아요 행만 지우면 기사의 좋아요 수(MongoDB)가 그대로 남으므로 수를 먼저 되돌린 뒤 삭제
    @Override
    @Transactional
    public void deleteAllByUser(String userId) {
        List<UserLikeEntity> likes = userLikeRepository.findByUserId(userId);
        likes.forEach(like -> newsArticleRepository.decreaseLikeCount(like.getArticleUrl()));
        userLikeRepository.deleteAll(likes);
    }
}
