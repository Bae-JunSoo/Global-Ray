package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.dto.ChatHistoryDto;
import kopo.poly.globalray.dto.ChatResponseDto;
import kopo.poly.globalray.dto.ChatSourceDto;
import kopo.poly.globalray.entity.ChatHistoryEntity;
import kopo.poly.globalray.entity.NewsArticleEntity;
import kopo.poly.globalray.repository.ChatHistoryRepository;
import kopo.poly.globalray.repository.NewsArticleRepository;
import kopo.poly.globalray.service.IChatBotService;
import kopo.poly.globalray.service.IGeminiService;
import kopo.poly.globalray.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBotServiceImpl implements IChatBotService {

    private static final int MAX_QUESTION_LENGTH = 300;
    private static final int MAX_KEYWORDS = 5;
    private static final int CANDIDATE_SIZE = 30;
    private static final int SOURCE_SIZE = 5;

    private static final Set<String> STOPWORDS = Set.of(
            "뉴스", "기사", "알려줘", "알려주세요", "알려", "관련", "관련된", "대해", "대해서", "대한",
            "최근", "요즘", "오늘", "뭐야", "뭐", "무엇", "어때", "어떻게", "어떤", "좀", "줘", "주세요",
            "있어", "있나요", "정리", "정리해줘", "요약", "요약해줘", "설명", "설명해줘",
            "이슈", "소식", "미치는", "궁금해", "궁금합니다"
    );

    // 긴 조사부터 검사해야 "에서"가 "서"보다 먼저 제거됨
    private static final List<String> PARTICLES = List.of(
            "에서는", "으로는", "에게서", "에서", "으로", "에게", "까지", "부터", "보다", "처럼", "이랑",
            "은", "는", "이", "가", "을", "를", "에", "의", "와", "과", "도", "만", "로", "랑"
    );

    private static final String NO_KEYWORD_ANSWER =
            "질문에서 검색할 키워드를 찾지 못했습니다.\n인물, 국가, 기업, 사건 이름처럼 구체적인 단어를 넣어 질문해주세요.";
    private static final String AI_UNAVAILABLE_ANSWER =
            "현재 AI 답변을 생성할 수 없습니다.\n대신 질문과 관련된 기사를 찾아드렸어요. 아래 기사를 확인해주세요.";
    private static final String NO_NEWS_ANSWER =
            "죄송합니다. 해당 주제와 관련된 뉴스를 찾을 수 없습니다.\nGlobalRay에 수집된 뉴스와 관련된 질문을 해주세요.";

    private final IGeminiService geminiService;
    private final NewsArticleRepository newsArticleRepository;
    private final ChatHistoryRepository chatHistoryRepository;

    // 수 초 걸리는 Gemini 호출 동안 DB 커넥션을 잡지 않도록 트랜잭션 미적용 (save()는 자체 트랜잭션)
    @Override
    public ChatResponseDto ask(String userId, String question) {
        String cleanQuestion = CmmUtil.nvl(question).trim();
        if (cleanQuestion.isEmpty()) {
            throw new IllegalArgumentException("질문을 입력해주세요.");
        }
        if (cleanQuestion.length() > MAX_QUESTION_LENGTH) {
            throw new IllegalArgumentException("질문은 " + MAX_QUESTION_LENGTH + "자 이내로 입력해주세요.");
        }

        List<String> keywords = extractKeywords(cleanQuestion);
        if (keywords.isEmpty()) {
            return ChatResponseDto.builder().answer(NO_KEYWORD_ANSWER).keywords(keywords).sources(List.of()).build();
        }

        List<NewsArticleEntity> relatedNews = findRelatedNews(keywords);
        log.info("챗봇 검색 - 키워드: {}, 관련 기사 수: {}", keywords, relatedNews.size());

        if (relatedNews.isEmpty()) {
            return ChatResponseDto.builder().answer(NO_NEWS_ANSWER).keywords(keywords).sources(List.of()).build();
        }

        List<ChatSourceDto> sources = relatedNews.stream().map(this::toSourceDto).toList();

        String answer = callGemini(buildPrompt(cleanQuestion, relatedNews));
        if (answer == null) {
            return ChatResponseDto.builder()
                    .answer(AI_UNAVAILABLE_ANSWER)
                    .aiAnswered(false)
                    .keywords(keywords)
                    .sources(sources)
                    .build();
        }

        chatHistoryRepository.save(ChatHistoryEntity.builder()
                .userId(userId)
                .question(cleanQuestion)
                .keywords(String.join(",", keywords))
                .answer(answer)
                .sourceCount(relatedNews.size())
                .build());

        return ChatResponseDto.builder()
                .answer(answer)
                .aiAnswered(true)
                .keywords(keywords)
                .sources(sources)
                .build();
    }

    // 사용량 초과·네트워크 오류 등으로 AI만 실패해도 검색된 기사는 보여줄 수 있도록 null로 돌려줌
    private String callGemini(String prompt) {
        try {
            String answer = geminiService.callGeminiApi(prompt);
            return (answer == null || answer.isBlank()) ? null : answer.trim();
        } catch (Exception e) {
            log.warn("챗봇 Gemini 호출 실패 - 관련 기사만 반환: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatHistoryDto> getHistory(String userId) {
        List<ChatHistoryDto> history = chatHistoryRepository.findTop20ByUserIdOrderByRegDtDesc(userId)
                .stream()
                .map(e -> ChatHistoryDto.builder()
                        .chatId(e.getChatId())
                        .question(e.getQuestion())
                        .answer(e.getAnswer())
                        .sourceCount(e.getSourceCount())
                        .regDt(e.getRegDt())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(history);
        return history;
    }

    @Override
    @Transactional
    public void clearHistory(String userId) {
        chatHistoryRepository.deleteByUserId(userId);
    }

    private List<String> extractKeywords(String question) {
        String normalized = question.replaceAll("[^0-9A-Za-z가-힣\\s]", " ").toLowerCase();

        return Arrays.stream(normalized.split("\\s+"))
                .map(this::stripParticle)
                .filter(word -> word.length() >= 2)
                .filter(word -> !STOPWORDS.contains(word))
                .distinct()
                .limit(MAX_KEYWORDS)
                .toList();
    }

    private String stripParticle(String word) {
        for (String particle : PARTICLES) {
            if (word.endsWith(particle) && word.length() - particle.length() >= 2) {
                return word.substring(0, word.length() - particle.length());
            }
        }
        return word;
    }

    // 키워드는 한글/영문/숫자만 남겨서 정규식 특수문자가 들어갈 수 없음
    private List<NewsArticleEntity> findRelatedNews(List<String> keywords) {
        String regex = String.join("|", keywords);
        List<NewsArticleEntity> candidates =
                newsArticleRepository.findByKeywordInTitleOrSummary(regex, PageRequest.of(0, CANDIDATE_SIZE));

        return candidates.stream()
                .sorted(Comparator.comparingInt((NewsArticleEntity n) -> score(n, keywords)).reversed())
                .limit(SOURCE_SIZE)
                .toList();
    }

    // 제목에 포함되면 2점, 요약에 포함되면 1점 (동점이면 최신순 유지: 후보가 이미 최신순 정렬됨)
    private int score(NewsArticleEntity news, List<String> keywords) {
        String title = CmmUtil.nvl(news.getTitleKor()).toLowerCase();
        String summary = CmmUtil.nvl(news.getSummaryKor()).toLowerCase();
        int score = 0;
        for (String keyword : keywords) {
            if (title.contains(keyword)) score += 2;
            if (summary.contains(keyword)) score += 1;
        }
        return score;
    }

    private String buildPrompt(String question, List<NewsArticleEntity> news) {
        String context = IntStream.range(0, news.size())
                .mapToObj(i -> "[" + (i + 1) + "] 제목: " + news.get(i).getTitleKor()
                        + "\n요약: " + CmmUtil.nvl(news.get(i).getSummaryKor(), CmmUtil.nvl(news.get(i).getDescription(), "")))
                .collect(Collectors.joining("\n\n"));

        return "아래 뉴스 기사들만 참고하여 질문에 한국어로 답변해주세요.\n"
                + "참고 기사에 없는 내용은 추측하지 말고 모른다고 답하세요.\n"
                + "근거로 사용한 기사 번호를 [1]처럼 문장 끝에 표시하세요.\n"
                + "마크다운 기호(###, ** 등)는 사용하지 마세요.\n\n"
                + "[참고 뉴스 기사]\n" + context + "\n\n[질문]\n" + question;
    }

    private ChatSourceDto toSourceDto(NewsArticleEntity news) {
        return ChatSourceDto.builder()
                .articleId(news.getArticleId())
                .title(news.getTitleKor())
                .sourceName(news.getSourceName())
                .build();
    }
}
