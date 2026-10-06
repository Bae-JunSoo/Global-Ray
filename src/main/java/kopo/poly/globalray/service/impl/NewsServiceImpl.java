package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.dto.NewsDto;
import kopo.poly.globalray.entity.NewsArticleEntity;
import kopo.poly.globalray.entity.UserBookmarkEntity;
import kopo.poly.globalray.entity.UserLikeEntity;
import kopo.poly.globalray.entity.ViewHistoryEntity;
import kopo.poly.globalray.repository.NewsArticleRepository;
import kopo.poly.globalray.repository.UserBookmarkRepository;
import kopo.poly.globalray.repository.UserLikeRepository;
import kopo.poly.globalray.repository.ViewHistoryRepository;
import kopo.poly.globalray.service.IGeminiService;
import kopo.poly.globalray.service.INewsService;
import kopo.poly.globalray.util.CmmUtil;
import kopo.poly.globalray.util.CountryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements INewsService {

    private final NewsArticleRepository newsArticleRepository;
    private final UserBookmarkRepository userBookmarkRepository;
    private final UserLikeRepository userLikeRepository;
    private final ViewHistoryRepository viewHistoryRepository;
    private final IGeminiService geminiService;

    private Set<String> getBookmarkedUrls(String loginUserId) {
        if (loginUserId == null) return new HashSet<>();
        return userBookmarkRepository.findByUserIdOrderByRegDtDesc(loginUserId)
                .stream()
                .map(UserBookmarkEntity::getArticleUrl)
                .collect(Collectors.toSet());
    }

    private Set<String> getLikedUrls(String loginUserId) {
        if (loginUserId == null) return new HashSet<>();
        return userLikeRepository.findByUserId(loginUserId)
                .stream()
                .map(UserLikeEntity::getArticleUrl)
                .collect(Collectors.toSet());
    }

    // 상세 요약은 비용이 커서 수집 때 만들지 않고, 처음 열람될 때 한 번 생성해 저장 (요약 저장이 있어 readOnly 아님)
    @Override
    @Transactional
    public NewsDto getArticleById(String articleId, String loginUserId) {
        NewsArticleEntity entity = newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("기사를 찾을 수 없습니다."));

        // summaryKor 없고 본문 있으면 on-demand 심화요약 생성
        if ((entity.getSummaryKor() == null || entity.getSummaryKor().isBlank())
                && entity.getContentFull() != null
                && !entity.getContentFull().isBlank()) {

            try {
                log.info("on-demand 심화요약 시작 : {}", entity.getTitleKor());

                String content = CmmUtil.truncate(entity.getContentFull(), 3000);

                // 수집 스케줄러가 한도를 다 써도 상세 요약은 동작하도록 별도 키(key2) 사용
                String summaryKor = geminiService.callGeminiApiWithKey2(
                        "다음 영문 뉴스 기사를 한국어로 번역하고 핵심 내용을 상세하게 요약해주세요. " +
                                "마크다운 기호(###, **, ## 등)를 절대 사용하지 말고 순수 텍스트로만 작성해주세요. " +
                                "300자 내외로 작성해주세요:\n\n" + content);

                if (summaryKor != null && !summaryKor.isBlank()) {
                    entity.updateSummaryKor(summaryKor.trim());
                    newsArticleRepository.save(entity);
                    log.info("on-demand 심화요약 완료 : {}", entity.getTitleKor());
                }

            } catch (Exception e) {
                // 요약 생성 실패해도 페이지는 정상 표시 (요약 없이 보여줌)
                log.warn("on-demand 심화요약 실패 : {}", e.getMessage());
            }
        }

        Set<String> bookmarkedUrls = getBookmarkedUrls(loginUserId);
        Set<String> likedUrls = getLikedUrls(loginUserId);
        return toDto(entity, bookmarkedUrls, likedUrls);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsDto> searchNews(String keyword, String loginUserId) {
        Set<String> bookmarkedUrls = getBookmarkedUrls(loginUserId);
        Set<String> likedUrls = getLikedUrls(loginUserId);
        return newsArticleRepository.findByTitleKorContainingOrderByRegDtDesc(
                        keyword,
                        Sort.by(Sort.Direction.DESC, "regDt"))
                .stream()
                .map(a -> toDto(a, bookmarkedUrls, likedUrls))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsDto> getBookmarkedNews(String userId) {
        List<UserBookmarkEntity> bookmarks = userBookmarkRepository.findByUserIdOrderByRegDtDesc(userId);
        if (bookmarks.isEmpty()) return List.of();

        List<String> urls = bookmarks.stream()
                .map(UserBookmarkEntity::getArticleUrl)
                .collect(Collectors.toList());

        Map<String, NewsArticleEntity> articleMap = newsArticleRepository.findByUrlIn(urls)
                .stream()
                .collect(Collectors.toMap(NewsArticleEntity::getUrl, Function.identity()));

        Set<String> bookmarkedUrlSet = new HashSet<>(urls);
        Set<String> likedUrls = getLikedUrls(userId);
        return bookmarks.stream()
                .map(bm -> articleMap.get(bm.getArticleUrl()))
                .filter(article -> article != null)
                .map(article -> toDto(article, bookmarkedUrlSet, likedUrls))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NewsDto> getNewsByCategory(String catType, int page, String loginUserId) {
        Pageable pageable = PageRequest.of(page, 10);
        Set<String> bookmarkedUrls = getBookmarkedUrls(loginUserId);
        Set<String> likedUrls = getLikedUrls(loginUserId);
        return newsArticleRepository
                .findByCatTypeAndTitleKorIsNotNullOrderByRegDtDesc(catType, pageable)
                .map(a -> toDto(a, bookmarkedUrls, likedUrls));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NewsDto> getNewsByCategory(String catType, int page, String loginUserId, String country) {
        Pageable pageable = PageRequest.of(page, 10);
        Set<String> bookmarkedUrls = getBookmarkedUrls(loginUserId);
        Set<String> likedUrls = getLikedUrls(loginUserId);
        List<String> sourceNames = CountryMapper.getSourceNames(country);
        return newsArticleRepository
                .findByCatTypeAndSourceNameInAndTitleKorIsNotNull(catType, sourceNames, pageable)
                .map(a -> toDto(a, bookmarkedUrls, likedUrls));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NewsDto> getMainNews(int page, String loginUserId) {
        Pageable pageable = PageRequest.of(page, 10);
        Set<String> bookmarkedUrls = getBookmarkedUrls(loginUserId);
        Set<String> likedUrls = getLikedUrls(loginUserId);
        return newsArticleRepository
                .findByTitleKorIsNotNullOrderByRegDtDesc(pageable)
                .map(a -> toDto(a, bookmarkedUrls, likedUrls));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NewsDto> getMainNews(int page, String loginUserId, String country) {
        Pageable pageable = PageRequest.of(page, 10);
        Set<String> bookmarkedUrls = getBookmarkedUrls(loginUserId);
        Set<String> likedUrls = getLikedUrls(loginUserId);
        List<String> sourceNames = CountryMapper.getSourceNames(country);
        return newsArticleRepository
                .findBySourceNameInAndTitleKorIsNotNull(sourceNames, pageable)
                .map(a -> toDto(a, bookmarkedUrls, likedUrls));
    }

    // 조회수 +1: Repository의 $inc 원자적 연산 위임
    @Override
    @Transactional
    public void increaseViewCount(String articleId) {
        newsArticleRepository.increaseViewCount(articleId);
    }

    @Override
    @Transactional
    public void saveViewHistory(String userId, String articleId, String title) {
        viewHistoryRepository.save(ViewHistoryEntity.builder()
                .userId((userId == null || userId.isBlank()) ? "비회원" : userId)
                .articleId(articleId)
                .title(title)
                .viewDt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsDto> getTop10ByViewCount() {
        return newsArticleRepository
                .findTop10ByTitleKorIsNotNullOrderByViewCountDesc(PageRequest.of(0, 10))
                .stream()
                .map(a -> toDto(a, new HashSet<>(), new HashSet<>()))
                .collect(Collectors.toList());
    }

    private NewsDto toDto(NewsArticleEntity article, Set<String> bookmarkedUrls, Set<String> likedUrls) {
        return NewsDto.builder()
                .articleId(article.getArticleId())
                .catType(article.getCatType())
                .title(article.getTitle())
                .titleKor(article.getTitleKor())
                .sourceName(article.getSourceName())
                .author(article.getAuthor())
                .url(article.getUrl())
                .description(article.getDescription())
                .summaryKor(article.getSummaryKor())
                .summaryShort(article.getSummaryShort())
                .thumbUrl(article.getThumbUrl())
                .regDt(article.getRegDt())
                .bookmarked(bookmarkedUrls.contains(article.getUrl()))
                .liked(likedUrls.contains(article.getUrl()))
                .viewCount(article.getViewCount())
                .likeCount(article.getLikeCount())
                .country(CountryMapper.getCountry(article.getSourceName()))
                .build();
    }
}
