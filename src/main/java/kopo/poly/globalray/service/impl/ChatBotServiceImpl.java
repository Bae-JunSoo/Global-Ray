package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.service.IChatBotService;
import kopo.poly.globalray.service.IGeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBotServiceImpl implements IChatBotService {

    private final IGeminiService geminiService;

    @Override
    public String askChatbot(String question) throws Exception {
        String prompt = "다음 질문에 친절하고 정확하게 한국어로 답변해주세요.\n\n질문: " + question;
        return geminiService.callGeminiApi(prompt);
    }
}