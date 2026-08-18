package kopo.poly.globalray.service;

public interface IChatBotService {

    // 챗봇 질문 응답 (Gemini API)
    String askChatbot(String question) throws Exception;
}