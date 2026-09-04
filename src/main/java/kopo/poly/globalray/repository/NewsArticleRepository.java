package kopo.poly.globalray.repository;

import kopo.poly.globalray.entity.NewsArticleEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Update;

public interface NewsArticleRepository extends MongoRepository<NewsArticleEntity, String> {

    // 카테고리별 조회 - titleKor 있는 기사만 (번역 완료된 기사만 노출)
    @Query(value = "{ 'CAT_TYPE': ?0, 'TITLE_KOR': { $exists: true, $ne: null } }", sort = "{ 'REG_DT': -1 }")
    List<NewsArticleEntity> findByCatTypeAndTitleKorIsNotNullOrderByRegDtDesc(String catType);

    // 카테고리별 최신 10개 - titleKor 있는 기사만
    @Query(value = "{ 'CAT_TYPE': ?0, 'TITLE_KOR': { $exists: true, $ne: null } }", sort = "{ 'REG_DT': -1 }")
    List<NewsArticleEntity> findTop10ByCatTypeAndTitleKorIsNotNullOrderByRegDtDesc(String catType);

    // 전체 페이징 - titleKor 있는 기사만 (메인 페이지용)
    @Query(value = "{ 'TITLE_KOR': { $exists: true, $ne: null } }", sort = "{ 'REG_DT': -1 }")
    Page<NewsArticleEntity> findByTitleKorIsNotNullOrderByRegDtDesc(Pageable pageable);

    // 카테고리별 페이징 - titleKor 있는 기사만
    @Query(value = "{ 'CAT_TYPE': ?0, 'TITLE_KOR': { $exists: true, $ne: null } }", sort = "{ 'REG_DT': -1 }")
    Page<NewsArticleEntity> findByCatTypeAndTitleKorIsNotNullOrderByRegDtDesc(String catType, Pageable pageable);

    // 한국어 제목 기준 키워드 검색 - 최신순 정렬 (Sort 파라미터로 제어)
    @Query("{ 'TITLE_KOR': { $regex: ?0, $options: 'i' } }")
    List<NewsArticleEntity> findByTitleKorContainingOrderByRegDtDesc(String keyword, Sort sort);

    // URL로 단건 조회 (북마크 기사 조회용)
    Optional<NewsArticleEntity> findByUrl(String url);

    // URL 목록으로 일괄 조회 (N+1 방지)
    List<NewsArticleEntity> findByUrlIn(List<String> urls);

    // 국가 필터: 전체 페이징 (sourceName 목록 기준)
    @Query(value = "{ 'SOURCE_NAME': { $in: ?0 }, 'TITLE_KOR': { $exists: true, $ne: null } }", sort = "{ 'REG_DT': -1 }")
    Page<NewsArticleEntity> findBySourceNameInAndTitleKorIsNotNull(List<String> sourceNames, Pageable pageable);

    // 국가 필터: 카테고리별 페이징
    @Query(value = "{ 'CAT_TYPE': ?0, 'SOURCE_NAME': { $in: ?1 }, 'TITLE_KOR': { $exists: true, $ne: null } }", sort = "{ 'REG_DT': -1 }")
    Page<NewsArticleEntity> findByCatTypeAndSourceNameInAndTitleKorIsNotNull(String catType, List<String> sourceNames, Pageable pageable);

    // URL 중복 체크 (수집 시 기존 기사 스킵용)
    boolean existsByUrl(String url);

    // 상세 페이지 on-demand 심화요약 대상 조회
    List<NewsArticleEntity> findBySummaryKorIsNullAndTitleKorIsNotNullAndContentFullIsNotNull();

    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'VIEW_COUNT': 1 } }")
    void increaseViewCount(String articleId);

    @Query("{ 'URL': ?0 }")
    @Update("{ '$inc': { 'LIKE_COUNT': 1 } }")
    void increaseLikeCount(String articleUrl);

    @Query("{ 'URL': ?0 }")
    @Update("{ '$inc': { 'LIKE_COUNT': -1 } }")
    void decreaseLikeCount(String articleUrl);

    // 조회수 TOP 10 (번역 완료 기사만, VIEW_COUNT 내림차순)
    // @Query 사용 시 메서드명의 Top10 제한이 무시되므로 Pageable로 10개 제한
    @Query(value = "{ 'TITLE_KOR': { $exists: true, $ne: null } }", sort = "{ 'VIEW_COUNT': -1 }")
    List<NewsArticleEntity> findTop10ByTitleKorIsNotNullOrderByViewCountDesc(Pageable pageable);
}