package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.entity.NewsArticleEntity;
import kopo.poly.globalray.repository.NewsArticleRepository;
import kopo.poly.globalray.service.IChatBotService;
import kopo.poly.globalray.service.IGeminiService;
import kopo.poly.globalray.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBotServiceImpl implements IChatBotService {

    private final IGeminiService geminiService;
    private final NewsArticleRepository newsArticleRepository;

    @Override
    public String askChatbot(String question) throws Exception {
        // 1. MongoDB에서 질문 키워드로 관련 뉴스 최대 5건 검색
        List<NewsArticleEntity> relatedNews =
                newsArticleRepository.findByKeywordInTitleOrSummary(question, PageRequest.of(0, 5));

        log.info("챗봇 관련 뉴스 검색 결과 - 질문: '{}', 검색된 기사 수: {}", question, relatedNews.size());

        if (relatedNews.isEmpty()) {
            // 관련 뉴스가 없으면 안내 메시지 반환 (일반 질문 차단)
            return "죄송합니다. 해당 주제와 관련된 뉴스를 찾을 수 없습니다.\nGlobalRay에 수집된 뉴스와 관련된 질문을 해주세요.";
        }

        // 검색된 뉴스 제목 + 요약을 컨텍스트로 구성
        String context = relatedNews.stream()
                .map(n -> "제목: " + n.getTitleKor()
                        + "\n요약: " + CmmUtil.nvl(n.getSummaryKor(), CmmUtil.nvl(n.getDescription(), "")))
                .collect(Collectors.joining("\n\n"));

        String prompt = "아래 뉴스 기사들을 참고하여 질문에 한국어로 답변해주세요.\n"
                + "반드시 참고 기사 내용을 바탕으로만 답변하고, 관련 없는 내용은 답하지 마세요.\n\n"
                + "[참고 뉴스 기사]\n" + context + "\n\n[질문]\n" + question;

        return geminiService.callGeminiApi(prompt);
    }
}
