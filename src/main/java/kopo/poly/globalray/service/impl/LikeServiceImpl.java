package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.entity.UserLikeEntity;
import kopo.poly.globalray.repository.NewsArticleRepository;
import kopo.poly.globalray.repository.UserLikeRepository;
import kopo.poly.globalray.service.ILikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional(readOnly = true)
    public boolean isLiked(String userId, String articleUrl) {
        return userLikeRepository.existsByUserIdAndArticleUrl(userId, articleUrl);
    }
}
