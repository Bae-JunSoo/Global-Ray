package kopo.poly.globalray.service;

import kopo.poly.globalray.dto.NewsDto;
import org.springframework.data.domain.Page;
import java.util.List;

public interface INewsService {

    // 카테고리별 뉴스 목록 조회
    List<NewsDto> getNewsByCategory(String catType, String loginUserId);

    // 카테고리별 최신 10개 조회 (메인 페이지용)
    List<NewsDto> getTop10ByCategory(String catType, String loginUserId);

    // 기사 상세 조회
    NewsDto getArticleById(String articleId, String loginUserId);

    // 키워드 검색
    List<NewsDto> searchNews(String keyword, String loginUserId);

    // 북마크한 기사 목록 조회
    List<NewsDto> getBookmarkedNews(String userId);

    // 카테고리별 페이징 조회
    Page<NewsDto> getNewsByCategory(String catType, int page, String loginUserId);

    // 메인 페이지용 페이징 조회
    Page<NewsDto> getMainNews(int page, String loginUserId);

    // 조회수 +1 (상세 페이지 진입 시 호출)
    void increaseViewCount(String articleId);

    // 조회수 TOP 10 기사 조회
    List<NewsDto> getTop10ByViewCount();
}