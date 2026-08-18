package kopo.poly.globalray.service;

public interface INewsCollectService {

    // 전체 카테고리 뉴스 수집 (배치용)
    void collectAllCategories() throws Exception;

    // 기사 본문 크롤링 (Jsoup)
    String crawlArticleContent(String url) throws Exception;
}