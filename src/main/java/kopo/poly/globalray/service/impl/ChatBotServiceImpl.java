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

        String prompt;
        if (relatedNews.isEmpty()) {
            // 관련 뉴스가 없으면 일반 AI 답변
            prompt = "다음 질문에 친절하고 정확하게 한국어로 답변해주세요.\n\n질문: " + question;
        } else {
            // 검색된 뉴스 제목 + 요약을 컨텍스트로 구성
            String context = relatedNews.stream()
                    .map(n -> "제목: " + n.getTitleKor()
                            + "\n요약: " + CmmUtil.nvl(n.getSummaryKor(), CmmUtil.nvl(n.getDescription(), "")))
                    .collect(Collectors.joining("\n\n"));

            prompt = "아래 뉴스 기사들을 참고하여 질문에 한국어로 답변해주세요.\n"
                    + "참고 기사와 관련이 없는 질문이면 일반적인 지식으로 답해주세요.\n\n"
                    + "[참고 뉴스 기사]\n" + context + "\n\n[질문]\n" + question;
        }

        return geminiService.callGeminiApi(prompt);
    }
}
