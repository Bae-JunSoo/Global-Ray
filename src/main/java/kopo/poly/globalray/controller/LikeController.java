package kopo.poly.globalray.controller;

import kopo.poly.globalray.service.ILikeService;
import kopo.poly.globalray.util.CmmUtil;
import kopo.poly.globalray.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LikeController {

    private final ILikeService likeService;

    @PostMapping("/like/toggle")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @RequestBody Map<String, String> body,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }
        String articleUrl = CmmUtil.nvl(body.get("articleUrl")).trim();
        if (articleUrl.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "기사 정보가 없습니다."));
        }

        boolean liked = likeService.toggleLike(SecurityUtil.extractUserId(principal), articleUrl);
        return ResponseEntity.ok(Map.of("liked", liked));
    }
}
