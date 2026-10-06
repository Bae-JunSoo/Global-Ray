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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements INewsService {

    private static final int PAGE_SIZE = 10;

    private final NewsArticleRepository newsArticleRepository;
    private final UserBookmarkRepository userBookmarkRepository;
    private final UserLikeRepository userLikeRepository;
    private final ViewHistoryRepository viewHistoryRepository;
    private final IGeminiService geminiService;

    // 카테고리와 국가 필터 조합에 맞는 쿼리를 고름 (catType이 비어 있으면 전체, country가 ALL이면 필터 없음)
    @Override
    @Transactional(readOnly = true)
    public Page<NewsDto> getNewsList(String catType, String country, int page, String loginUserId) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
        boolean hasCategory = catType != null && !catType.isBlank();
        boolean hasCountry = country != null && !country.isBlank() && !"ALL".equals(country);
        List<String> sourceNames = hasCountry ? CountryMapper.getSourceNames(country) : List.of();

        Page<NewsArticleEntity> articles;
        if (hasCategory && hasCountry) {
            articles = newsArticleRepository.findByCatTypeAndSourceNameInAndTitleKorIsNotNull(catType, sourceNames, pageable);
        } else if (hasCategory) {
            articles = newsArticleRepository.findByCatTypeAndTitleKorIsNotNullOrderByRegDtDesc(catType, pageable);
        } else if (hasCountry) {
            articles = newsArticleRepository.findBySourceNameInAndTitleKorIsNotNull(sourceNames, pageable);
        } else {
            articles = newsArticleRepository.findByTitleKorIsNotNullOrderByRegDtDesc(pageable);
        }
        return articles.map(dtoMapper(loginUserId));
    }

    // 조회수 증가 → 상세 조회(필요 시 요약 생성) → 열람 이력 저장을 한 흐름으로 처리
    // 트랜잭션은 MariaDB의 북마크·좋아요 조회에만 적용되고, MongoDB 쓰기는 연산 하나하나가 원자적으로 처리됨
    @Override
    @Transactional
    public NewsDto viewArticle(String articleId, String loginUserId) {
        // 상세 화면에 증가된 조회수가 보이도록 먼저 증가시킴 ($inc는 없는 기사면 아무 일도 하지 않음)
        newsArticleRepository.increaseViewCount(articleId);

        NewsArticleEntity entity = newsArticleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("기사를 찾을 수 없습니다."));

        generateSummaryIfAbsent(entity);
        saveViewHistory(loginUserId, entity);

        return dtoMapper(loginUserId).apply(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsDto> searchNews(String keyword, String loginUserId) {
        // 검색어가 그대로 정규식에 들어가면 "(" 같은 문자로 쿼리 오류가 나므로 일반 문자열로 감쌈
        String safeKeyword = Pattern.quote(CmmUtil.nvl(keyword).trim());
        return newsArticleRepository.findByTitleKorContainingOrderByRegDtDesc(
                        safeKeyword, Sort.by(Sort.Direction.DESC, "regDt"))
                .stream()
                .map(dtoMapper(loginUserId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsDto> getBookmarkedNews(String userId) {
        List<UserBookmarkEntity> bookmarks = userBookmarkRepository.findByUserIdOrderByRegDtDesc(userId);
        if (bookmarks.isEmpty()) return List.of();

        List<String> urls = bookmarks.stream()
                .map(UserBookmarkEntity::getArticleUrl)
                .toList();

        // 기사를 한 건씩 조회하지 않고 URL 목록으로 한 번에 가져와 N+1 쿼리를 방지
        Map<String, NewsArticleEntity> articleMap = newsArticleRepository.findByUrlIn(urls)
                .stream()
                .collect(Collectors.toMap(NewsArticleEntity::getUrl, Function.identity()));

        Function<NewsArticleEntity, NewsDto> mapper = dtoMapper(userId);
        return bookmarks.stream()
                .map(bm -> articleMap.get(bm.getArticleUrl()))
                .filter(Objects::nonNull)
                .map(mapper)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsDto> getTop10ByViewCount() {
        return newsArticleRepository
                .findTop10ByTitleKorIsNotNullOrderByViewCountDesc(PageRequest.of(0, 10))
                .stream()
                .map(article -> toDto(article, Set.of(), Set.of()))
                .toList();
    }

    // 상세 요약은 비용이 커서 수집 때 만들지 않고, 처음 열람될 때 한 번 생성해 저장
    private void generateSummaryIfAbsent(NewsArticleEntity entity) {
        boolean hasSummary = entity.getSummaryKor() != null && !entity.getSummaryKor().isBlank();
        boolean hasContent = entity.getContentFull() != null && !entity.getContentFull().isBlank();
        if (hasSummary || !hasContent) return;

        try {
            String content = CmmUtil.truncate(entity.getContentFull(), 3000);
            // 수집 스케줄러가 한도를 다 써도 상세 요약은 동작하도록 별도 키(key2) 사용
            String summaryKor = geminiService.callGeminiApiWithKey2(
                    "다음 영문 뉴스 기사를 한국어로 번역하고 핵심 내용을 상세하게 요약해주세요. " +
                            "마크다운 기호(###, **, ## 등)를 절대 사용하지 말고 순수 텍스트로만 작성해주세요. " +
                            "300자 내외로 작성해주세요:\n\n" + content);

            if (summaryKor != null && !summaryKor.isBlank()) {
                entity.updateSummaryKor(summaryKor.trim());
                newsArticleRepository.save(entity);
                log.info("상세 요약 생성 완료 : {}", entity.getTitleKor());
            }
        } catch (Exception e) {
            // 요약 생성에 실패해도 기사 화면은 요약 없이 정상 표시
            log.warn("상세 요약 생성 실패 : {}", e.getMessage());
        }
    }

    private void saveViewHistory(String loginUserId, NewsArticleEntity entity) {
        viewHistoryRepository.save(ViewHistoryEntity.builder()
                .userId((loginUserId == null || loginUserId.isBlank()) ? "비회원" : loginUserId)
                .articleId(entity.getArticleId())
                .title(entity.getTitleKor() != null ? entity.getTitleKor() : entity.getTitle())
                .viewDt(LocalDateTime.now())
                .build());
    }

    // 목록 조회마다 반복되던 "내 북마크·좋아요 목록 조회 후 DTO 변환"을 한 곳으로 모음
    private Function<NewsArticleEntity, NewsDto> dtoMapper(String loginUserId) {
        Set<String> bookmarkedUrls = getBookmarkedUrls(loginUserId);
        Set<String> likedUrls = getLikedUrls(loginUserId);
        return article -> toDto(article, bookmarkedUrls, likedUrls);
    }

    private Set<String> getBookmarkedUrls(String loginUserId) {
        if (loginUserId == null) return Set.of();
        return userBookmarkRepository.findByUserIdOrderByRegDtDesc(loginUserId)
                .stream()
                .map(UserBookmarkEntity::getArticleUrl)
                .collect(Collectors.toSet());
    }

    private Set<String> getLikedUrls(String loginUserId) {
        if (loginUserId == null) return Set.of();
        return userLikeRepository.findByUserId(loginUserId)
                .stream()
                .map(UserLikeEntity::getArticleUrl)
                .collect(Collectors.toSet());
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
