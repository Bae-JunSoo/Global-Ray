package kopo.poly.globalray.controller;

import kopo.poly.globalray.dto.ChatHistoryDto;
import kopo.poly.globalray.dto.ChatRequestDto;
import kopo.poly.globalray.service.IChatBotService;
import kopo.poly.globalray.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatBotController {

    private final IChatBotService chatBotService;

    @GetMapping
    public String chatbotPage() {
        return "chatbot/index";
    }

    @PostMapping("/ask")
    @ResponseBody
    public ResponseEntity<?> ask(@RequestBody ChatRequestDto request, Principal principal) {
        String userId = SecurityUtil.extractUserId(principal);
        try {
            return ResponseEntity.ok(chatBotService.ask(userId, request.getQuestion()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("챗봇 AI 호출 실패 - userId: {}, 원인: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "AI 답변을 가져오지 못했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    @GetMapping("/history")
    @ResponseBody
    public List<ChatHistoryDto> history(Principal principal) {
        return chatBotService.getHistory(SecurityUtil.extractUserId(principal));
    }

    @DeleteMapping("/history")
    @ResponseBody
    public ResponseEntity<Void> clearHistory(Principal principal) {
        chatBotService.clearHistory(SecurityUtil.extractUserId(principal));
        return ResponseEntity.noContent().build();
    }
}
