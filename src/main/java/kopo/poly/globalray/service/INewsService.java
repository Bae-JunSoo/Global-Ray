package kopo.poly.globalray.service;

import kopo.poly.globalray.dto.NewsDto;
import org.springframework.data.domain.Page;
import java.util.List;

public interface INewsService {

    // 뉴스 목록 페이징 조회 (catType이 비어 있으면 전체, country가 ALL이면 국가 필터 없음)
    Page<NewsDto> getNewsList(String catType, String country, int page, String loginUserId);

    // 기사 열람: 조회수 증가 + 상세 조회 + 열람 이력 저장
    NewsDto viewArticle(String articleId, String loginUserId);

    // 키워드 검색
    List<NewsDto> searchNews(String keyword, String loginUserId);

    // 북마크한 기사 목록 조회
    List<NewsDto> getBookmarkedNews(String userId);

    // 조회수 TOP 10 기사 조회
    List<NewsDto> getTop10ByViewCount();
}
