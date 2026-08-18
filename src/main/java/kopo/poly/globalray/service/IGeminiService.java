package kopo.poly.globalray.service;

public interface IGeminiService {

    // AI 요약 미처리 기사 일괄 처리 (배치용)
    void processUnSummarizedArticles() throws Exception;

    // Gemini API 호출 (스케줄러용 기존 키)
    String callGeminiApi(String prompt) throws Exception;

    // Gemini API 호출 (심화요약용 새 키)
    String callGeminiApiWithKey2(String prompt) throws Exception;
}