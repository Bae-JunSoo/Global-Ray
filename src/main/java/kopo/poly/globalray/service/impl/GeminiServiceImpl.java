package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.service.IGeminiService;
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

    private final WebClient.Builder webClientBuilder;

    @Value("${api.gemini.key}")
    private String geminiKey;

    @Value("${api.gemini.key2}")
    private String geminiKey2;

    @Value("${api.gemini.url}")
    private String geminiUrl;

    @Override
    public String callGeminiApi(String prompt) throws Exception {
        return callGeminiApiWithKey(prompt, geminiKey);
    }

    @Override
    public String callGeminiApiWithKey2(String prompt) throws Exception {
        return callGeminiApiWithKey(prompt, geminiKey2);
    }

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
        if (content == null) return null;

        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) return null;

        Object text = parts.get(0).get("text");
        return text instanceof String s ? s : null;
    }
}
