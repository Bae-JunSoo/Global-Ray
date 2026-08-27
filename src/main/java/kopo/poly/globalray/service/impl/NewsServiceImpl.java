package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.dto.NewsDto;
import kopo.poly.globalray.entity.NewsArticleEntity;
import kopo.poly.globalray.entity.UserBookmarkEntity;
import kopo.poly.globalray.repository.NewsArticleRepository;
import kopo.poly.globalray.repository.UserBookmarkRepository;
import kopo.poly.globalray.service.IGeminiService;
import kopo.poly.globalray.service.INewsService;
import kopo.poly.globalray.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements INewsService {

    private final NewsArticleRepository newsArticleRepository;
    private final UserBookmarkRepository userBookmarkRepository;

    /**
     * [추가] IGeminiService 주입
     * on-demand 심화요약 생성 로직을 NewsController 에서 이 Service 로 이동했기 때문
     * Controller 는 DB/외부 API 에 직접 의존하면 안 됨 (계층 구조 위반)
     */
    private final IGeminiService geminiService;

    // 유저의 북마크 URL 목록을 한 번에 조회 (N+1 방지)
    @Transactional(readOnly = true)
    protected Set<String> getBookmarkedUrls(String loginUserId) {
        if (loginUserId == null) return new HashSet<>();
        return userBookmarkRepository.findByUserIdOrderByRegDtDesc(loginUserId)
                .stream()
                .map(UserBookmarkEntity::getArticleUrl)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsDto> getNewsByCategory(String catType, String loginUserId) {
        Set<String> bookmarkedUrls = getBookmarkedUrls(loginUserId);
        return newsArticleRepository
                .findByCatTypeAndTitleKorIsNotNullOrderByRegDtDesc(catType)
                .stream()
                .map(a -> toDto(a, bookmarkedUrls))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsDto> getTop10ByCategory(String catType, String loginUserId) {
        Set<String> bookmarkedUrls = getBookmarkedUrls(loginUserId);
        return newsArticleRepository
                .findTop10ByCatTypeAndTitleKorIsNotNullOrderByRegDtDesc(catType)
                .stream()
                .map(a -> toDto(a, bookmarkedUrls))
                .collect(Collectors.toList());
    }

    /**
     * 기사 상세 조회 + on-demand 심화요약 자동 생성
     *
     * [이전 문제]
     * on-demand 심화요약 생성 로직이 NewsController.newsDetail() 안에 있었음
     * - Controller 가 NewsArticleRepository 에 직접 의존 (계층 위반)
     * - Controller 가 IGeminiService 에 직접 의존 (외부 API 호출이 Controller 에)
     * - "요약이 없으면 만들어라" 는 비즈니스 규칙이 표현 계층에 노출됨
     *
     * [해결]
     * Service 레이어로 이동: Controller 는 getArticleById() 결과만 받아 View 에 전달
     * 요약 생성 여부 판단, Gemini 호출, DB 저장이 모두 Service 안에서 처리됨
     *
     * [트랜잭션]
     * summaryKor 저장이 있으므로 readOnly = false (기본값) 사용
     */
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

                // 심화요약 전용 키(key2) 사용 → 스케줄러 번역 API 키와 분리
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
        return toDto(entity, bookmarkedUrls);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsDto> searchNews(String keyword, String loginUserId) {
        Set<String> bookmarkedUrls = getBookmarkedUrls(loginUserId);
        return newsArticleRepository.findByTitleKorContainingOrderByRegDtDesc(
                        keyword,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "regDt"))
                .stream()
                .map(a -> toDto(a, bookmarkedUrls))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsDto> getBookmarkedNews(String userId) {
        Set<String> bookmarkedUrls = getBookmarkedUrls(userId);
        return userBookmarkRepository.findByUserIdOrderByRegDtDesc(userId)
                .stream()
                .map(bm -> {
                    NewsArticleEntity article = newsArticleRepository
                            .findByUrl(bm.getArticleUrl()).orElse(null);
                    if (article == null) return null;
                    return toDto(article, bookmarkedUrls);
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NewsDto> getNewsByCategory(String catType, int page, String loginUserId) {
        Pageable pageable = PageRequest.of(page, 10);
        Set<String> bookmarkedUrls = getBookmarkedUrls(loginUserId);
        return newsArticleRepository
                .findByCatTypeAndTitleKorIsNotNullOrderByRegDtDesc(catType, pageable)
                .map(a -> toDto(a, bookmarkedUrls));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NewsDto> getMainNews(int page, String loginUserId) {
        Pageable pageable = PageRequest.of(page, 10);
        Set<String> bookmarkedUrls = getBookmarkedUrls(loginUserId);
        return newsArticleRepository
                .findByTitleKorIsNotNullOrderByRegDtDesc(pageable)
                .map(a -> toDto(a, bookmarkedUrls));
    }

    // 조회수 +1: Repository의 $inc 원자적 연산 위임
    @Override
    public void increaseViewCount(String articleId) {
        newsArticleRepository.increaseViewCount(articleId);
    }

    // 조회수 TOP 10: 비로그인 사용자도 볼 수 있으므로 북마크 Set 빈 값으로 처리
    @Override
    @Transactional(readOnly = true)
    public List<NewsDto> getTop10ByViewCount() {
        return newsArticleRepository
                .findTop10ByTitleKorIsNotNullOrderByViewCountDesc(PageRequest.of(0, 10))
                .stream()
                .map(a -> toDto(a, new HashSet<>()))
                .collect(Collectors.toList());
    }

    // Entity → DTO 변환 (북마크 Set 으로 비교 → N+1 쿼리 방지)
    private NewsDto toDto(NewsArticleEntity article, Set<String> bookmarkedUrls) {
        boolean bookmarked = bookmarkedUrls.contains(article.getUrl());
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
                .bookmarked(bookmarked)
                .viewCount(article.getViewCount())
                .build();
    }
}
