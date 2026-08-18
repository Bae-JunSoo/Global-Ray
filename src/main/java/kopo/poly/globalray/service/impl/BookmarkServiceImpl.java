package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.entity.UserBookmarkEntity;
import kopo.poly.globalray.repository.UserBookmarkRepository;
import kopo.poly.globalray.service.IBookmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements IBookmarkService {

    private final UserBookmarkRepository userBookmarkRepository;

    @Override
    @Transactional
    public boolean toggleBookmark(String userId, String articleUrl) {
        if (userBookmarkRepository.existsByUserIdAndArticleUrl(userId, articleUrl)) {
            userBookmarkRepository.findByUserIdAndArticleUrl(userId, articleUrl)
                    .ifPresent(userBookmarkRepository::delete);
            log.info("북마크 해제 - userId: {}", userId);
            return false;
        } else {
            UserBookmarkEntity bm = UserBookmarkEntity.builder()
                    .userId(userId)
                    .articleUrl(articleUrl)
                    .build();
            userBookmarkRepository.save(bm);
            log.info("북마크 추가 - userId: {}", userId);
            return true;
        }
    }
}