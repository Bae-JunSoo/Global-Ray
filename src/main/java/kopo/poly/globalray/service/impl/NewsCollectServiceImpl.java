package kopo.poly.globalray.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.globalray.entity.NewsArticleEntity;
import kopo.poly.globalray.repository.NewsArticleRepository;
import kopo.poly.globalray.service.IGeminiService;
import kopo.poly.globalray.service.INewsCollectService;
import kopo.poly.globalray.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@Slf4j
@Service
public class NewsCollectServiceImpl implements INewsCollectService {

    private final NewsArticleRepository newsArticleRepository;
    private final WebClient.Builder webClientBuilder;
    private final IGeminiService geminiService;

    private final ThreadPoolTaskExecutor crawlExecutor;
    private final ObjectMapper objectMapper;

    // @RequiredArgsConstructor는 생성자 파라미터에 @Qualifier를 붙여주지 않아 직접 작성
    public NewsCollectServiceImpl(
            NewsArticleRepository newsArticleRepository,
            WebClient.Builder webClientBuilder,
            IGeminiService geminiService,
            @Qualifier("crawlExecutor") ThreadPoolTaskExecutor crawlExecutor,
            ObjectMapper objectMapper) {
        this.newsArticleRepository = newsArticleRepository;
        this.webClientBuilder      = webClientBuilder;
        this.geminiService         = geminiService;
        this.crawlExecutor         = crawlExecutor;
        this.objectMapper          = objectMapper;
    }

    @Value("${api.news.key}")
    private String newsApiKey;

    @Value("${api.news.url}")
    private String newsApiUrl;

    // Gemini 무료 등급의 분당 호출 제한(15회)을 넘지 않도록 4초 간격으로 호출
    // 429 발생 시 분 단위 한도가 풀리도록 60초 대기, 최대 3회까지 시도
    private static final long GEMINI_INTERVAL_MS   = 4_000L;
    private static final long GEMINI_RETRY_WAIT_MS = 60_000L;
    private static final int  GEMINI_MAX_RETRY     = 3;

    // 카테고리 한국어 → News API 영어 파라미터 매핑
    // LinkedHashMap: 선언 순서대로 순회 보장 (Map.of() 는 순서 미보장)
    private static final Map<String, String> CATEGORY_MAP;
    static {
        CATEGORY_MAP = new LinkedHashMap<>();
        CATEGORY_MAP.put("경제",        "business");
        CATEGORY_MAP.put("엔터테인먼트", "entertainment");
        CATEGORY_MAP.put("종합",        "general");
        CATEGORY_MAP.put("보건",        "health");
        CATEGORY_MAP.put("과학",        "science");
        CATEGORY_MAP.put("IT",          "technology");
        CATEGORY_MAP.put("스포츠",      "sports");
    }

    // 크롤링 결과를 담는 내부 클래스
    private static class ArticleData {
        Map<String, Object> raw;
        String fullContent;
        String korCat;

        ArticleData(Map<String, Object> raw, String fullContent, String korCat) {
            this.raw         = raw;
            this.fullContent = fullContent;
            this.korCat      = korCat;
        }
    }

    @Override
    public void collectAllCategories() throws Exception {
        for (Map.Entry<String, String> entry : CATEGORY_MAP.entrySet()) {
            try {
                collectByCategory(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.error("뉴스 수집 실패 [{}] : {}", entry.getKey(), e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void collectByCategory(String korCat, String engCat) throws Exception {
        String url = newsApiUrl + "?category=" + engCat
                + "&language=en&pageSize=10&apiKey=" + newsApiKey;

        Map<String, Object> response = webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !"ok".equals(response.get("status"))) {
            log.warn("News API 응답 이상 : {}", korCat);
            return;
        }

        List<Map<String, Object>> articles =
                (List<Map<String, Object>>) response.get("articles");
        if (articles == null || articles.isEmpty()) return;

        // ── 1단계: 신규 기사만 필터링 ──
        List<Map<String, Object>> newArticles = new ArrayList<>();
        for (Map<String, Object> art : articles) {
            String articleUrl = (String) art.get("url");
            if (articleUrl == null || newsArticleRepository.existsByUrl(articleUrl)) continue;
            newArticles.add(art);
        }

        if (newArticles.isEmpty()) {
            log.info("[{}] 신규 저장 0건 완료", korCat);
            return;
        }

        log.info("[{}] 신규 기사 {}건 크롤링 시작", korCat, newArticles.size());

        // ── 2단계: 크롤링 병렬 처리 ──
        List<Future<ArticleData>> futures = new ArrayList<>();

        for (Map<String, Object> art : newArticles) {
            futures.add(crawlExecutor.submit(() -> {
                String articleUrl = (String) art.get("url");
                String fullContent = crawlArticleContent(articleUrl);
                return new ArticleData(art, fullContent, korCat);
            }));
        }

        // 크롤링 결과 수집 (모든 Future 완료 대기)
        List<ArticleData> crawledList = new ArrayList<>();
        for (Future<ArticleData> future : futures) {
            try {
                crawledList.add(future.get());
            } catch (Exception e) {
                log.warn("크롤링 Future 오류 : {}", e.getMessage());
            }
        }

        log.info("[{}] 크롤링 완료 → Gemini 번역 시작", korCat);

        // ── 3단계: Gemini 번역/요약 순서대로 처리 (4초 간격 유지) ──
        int newCount = 0;

        for (ArticleData data : crawledList) {
            String articleUrl  = (String) data.raw.get("url");
            Map<String, Object> source = (Map<String, Object>) data.raw.get("source");
            String sourceName  = source != null ? (String) source.get("name") : null;
            String title       = (String) data.raw.get("title");
            String description = (String) data.raw.get("description");

            String prompt = buildPrompt(title, description, data.fullContent);
            Map<String, String> translated = callGeminiWithRetry(prompt);

            // 번역 실패 시 저장하지 않음 (영어 기사가 메인에 노출되는 것 방지)
            if (translated == null) {
                log.warn("번역 실패 - 저장 스킵 : {}", title);
                continue;
            }

            NewsArticleEntity article = NewsArticleEntity.builder()
                    .catType(korCat)
                    .title(title)
                    .titleKor(translated.get("titleKor"))
                    .sourceName(sourceName)
                    .author((String) data.raw.get("author"))
                    .url(articleUrl)
                    .description(description)
                    .summaryShort(translated.get("summaryShort"))
                    .thumbUrl((String) data.raw.get("urlToImage"))
                    .contentFull(data.fullContent)
                    .regDt(LocalDateTime.now())
                    .build();

            newsArticleRepository.save(article);
            newCount++;
            log.info("뉴스 저장 완료 [{}/{}] : {}", korCat, newCount, translated.get("titleKor"));

            // Gemini API 호출 간격 4초 (분당 15회 제한 초과 방지)
            Thread.sleep(GEMINI_INTERVAL_MS);
        }

        log.info("[{}] 신규 저장 {}건 완료", korCat, newCount);
    }

    // 제목번역 + 3줄요약을 JSON 1번 호출로 처리하는 프롬프트 생성
    private String buildPrompt(String title, String description, String content) {
        String source = (content != null && !content.isBlank())
                ? CmmUtil.truncate(content, 3000)
                : (description != null ? description : title);

        return """
                아래 영문 뉴스를 분석해서 반드시 아래 JSON 형식으로만 응답하세요.
                다른 말은 절대 추가하지 마세요. JSON만 출력하세요.

                {
                  "titleKor": "한국어로 번역한 제목",
                  "summaryShort": "• 핵심 내용 1줄\\n• 핵심 내용 2줄\\n• 핵심 내용 3줄"
                }

                제목: %s
                본문: %s
                """.formatted(title, source);
    }

    // 429(호출 한도 초과)는 60초, 503(일시 오류)은 10초 기다렸다 재시도하고, 그 외 오류는 재시도해도 의미 없어 바로 실패 처리
    private Map<String, String> callGeminiWithRetry(String prompt) {
        for (int attempt = 1; attempt <= GEMINI_MAX_RETRY; attempt++) {
            try {
                String raw = geminiService.callGeminiApi(prompt);
                if (raw == null || raw.isBlank()) return null;

                // Gemini 가 ```json ... ``` 마크다운으로 감싸는 경우 제거
                String cleaned = raw
                        .replaceAll("(?s)```json\\s*", "")
                        .replaceAll("```", "")
                        .trim();

                // Jackson ObjectMapper 로 안전하게 JSON 파싱
                Map<String, String> result = objectMapper.readValue(
                        cleaned, new TypeReference<Map<String, String>>() {});

                // titleKor 없으면 번역 실패 처리
                if (result.get("titleKor") == null || result.get("titleKor").isBlank()) return null;

                // summaryShort 없으면 빈 문자열로 대체
                if (!result.containsKey("summaryShort")) result.put("summaryShort", "");

                return result;

            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";

                if (msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED")) {
                    log.warn("Gemini 429 Rate Limit (attempt {}/{}) → {}초 대기",
                            attempt, GEMINI_MAX_RETRY, GEMINI_RETRY_WAIT_MS / 1000);
                    if (attempt < GEMINI_MAX_RETRY) {
                        try { Thread.sleep(GEMINI_RETRY_WAIT_MS); }
                        catch (InterruptedException ie) { Thread.currentThread().interrupt(); return null; }
                    }
                } else if (msg.contains("503") || msg.contains("Service Unavailable")) {
                    log.warn("Gemini 503 일시 오류 (attempt {}/{}) → 10초 대기", attempt, GEMINI_MAX_RETRY);
                    if (attempt < GEMINI_MAX_RETRY) {
                        try { Thread.sleep(10_000L); }
                        catch (InterruptedException ie) { Thread.currentThread().interrupt(); return null; }
                    }
                } else {
                    log.warn("Gemini 호출 실패 (attempt {}) : {}", attempt, msg);
                    return null;
                }
            }
        }
        log.error("Gemini 최대 재시도({}) 초과 - 저장 스킵", GEMINI_MAX_RETRY);
        return null;
    }

    // Jsoup 으로 기사 본문 크롤링 (실패해도 수집은 계속되도록 null 반환)
    private String crawlArticleContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();

            String[] selectors = {
                    "article", "div.article-body",
                    "div.content", "main", "div.post-content"
            };
            for (String sel : selectors) {
                String text = doc.select(sel).text();
                if (text.length() > 200) return text;
            }
            return doc.body().text();
        } catch (Exception e) {
            log.warn("크롤링 실패 [{}] : {}", url, e.getMessage());
            return null;
        }
    }
}
