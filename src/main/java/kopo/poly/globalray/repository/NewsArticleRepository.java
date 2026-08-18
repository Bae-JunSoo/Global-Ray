package kopo.poly.globalray.repository;

import kopo.poly.globalray.entity.NewsArticleEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    // URL 중복 체크 (수집 시 기존 기사 스킵용)
    boolean existsByUrl(String url);

    // 상세 페이지 on-demand 심화요약 대상 조회
    List<NewsArticleEntity> findBySummaryKorIsNullAndTitleKorIsNotNullAndContentFullIsNotNull();
}