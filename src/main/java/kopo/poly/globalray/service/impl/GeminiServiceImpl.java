package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.entity.NewsArticleEntity;
import kopo.poly.globalray.repository.NewsArticleRepository;
import kopo.poly.globalray.service.IGeminiService;
import kopo.poly.globalray.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements IGeminiService {

    private final NewsArticleRepository newsArticleRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${api.gemini.key}")
    private String geminiKey;  // 스케줄러용 기존 키

    @Value("${api.gemini.key2}")
    private String geminiKey2;  // 심화요약용 새 키

    @Value("${api.gemini.url}")
    private String geminiUrl;

    // 상세 페이지 on-demand 심화요약 처리용
    // titleKor 있고(번역 완료) + summaryKor 없고 + 본문 있는 기사만 대상
    @Override
    public void processUnSummarizedArticles() throws Exception {
        List<NewsArticleEntity> articles =
                newsArticleRepository.findBySummaryKorIsNullAndTitleKorIsNotNullAndContentFullIsNotNull();
        log.info("AI 심화요약 대상 기사 수 : {}", articles.size());

        for (NewsArticleEntity article : articles) {
            try {
                String content = CmmUtil.truncate(article.getContentFull(), 3000);
                if (content.isBlank()) {
                    log.warn("빈 본문 스킵 : {}", article.getTitle());
                    continue;
                }

                // 심화요약은 새 키(key2) 사용
                String summaryKor = callGeminiApiWithKey(
                        "다음 영문 뉴스 기사를 한국어로 번역하고 핵심 내용을 상세하게 요약해주세요. " +
                                "마크다운 기호(###, **, ## 등)를 절대 사용하지 말고 순수 텍스트로만 작성해주세요. " +
                                "300자 내외로 작성해주세요:\n\n" + content,
                        geminiKey2);

                if (summaryKor == null) {
                    log.warn("심화요약 실패 스킵 : {}", article.getTitle());
                    Thread.sleep(4000);
                    continue;
                }

                article.updateSummaryKor(summaryKor);
                newsArticleRepository.save(article);
                log.info("심화요약 완료 : {}", article.getTitle());

                Thread.sleep(4000);

            } catch (Exception e) {
                log.error("심화요약 실패 [{}] : {}", article.getTitle(), e.getMessage());
                try { Thread.sleep(10000); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // 기존 키로 Gemini API 호출 (스케줄러용)
    @Override
    public String callGeminiApi(String prompt) throws Exception {
        return callGeminiApiWithKey(prompt, geminiKey);
    }

    // 새 키로 Gemini API 호출 (심화요약용)
    public String callGeminiApiWithKey2(String prompt) throws Exception {
        return callGeminiApiWithKey(prompt, geminiKey2);
    }

    // Gemini API 호출 공통 메서드
    @SuppressWarnings("unchecked")
    private String callGeminiApiWithKey(String prompt, String key) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(geminiUrl + "?key=" + key)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) return null;

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) return null;

        Map<String, Object> content =
                (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) return null;

        return (String) parts.get(0).get("text");
    }
}