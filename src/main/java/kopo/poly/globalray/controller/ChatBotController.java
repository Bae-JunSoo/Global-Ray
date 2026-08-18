package kopo.poly.globalray.controller;

import kopo.poly.globalray.service.IChatBotService;
import kopo.poly.globalray.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatBotController {

    private final IChatBotService chatBotService;

    // 챗봇 페이지
    @GetMapping
    public String chatbotPage() {
        return "chatbot/index";
    }

    // 챗봇 질문 (Ajax)
    @PostMapping("/ask")
    @ResponseBody
    public ResponseEntity<Map<String, String>> ask(@RequestBody Map<String, String> body) {
        String question = CmmUtil.nvl(body.get("question"));

        if (question.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("answer", "질문을 입력해주세요."));
        }

        try {
            String answer = chatBotService.askChatbot(question);
            return ResponseEntity.ok(
                    Map.of("answer", CmmUtil.nvl(answer, "답변을 가져오지 못했습니다."))
            );
        } catch (Exception e) {
            log.error("챗봇 오류 : {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("answer", "오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
        }
    }
}