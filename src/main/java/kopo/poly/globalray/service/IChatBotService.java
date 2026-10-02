package kopo.poly.globalray.service;

import kopo.poly.globalray.dto.ChatHistoryDto;
import kopo.poly.globalray.dto.ChatResponseDto;

import java.util.List;

public interface IChatBotService {

    ChatResponseDto ask(String userId, String question) throws Exception;

    List<ChatHistoryDto> getHistory(String userId);

    void clearHistory(String userId);
}
