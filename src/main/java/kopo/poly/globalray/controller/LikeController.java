package kopo.poly.globalray.controller;

import kopo.poly.globalray.service.ILikeService;
import kopo.poly.globalray.util.CmmUtil;
import kopo.poly.globalray.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
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
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        boolean liked = likeService.toggleLike(
                SecurityUtil.extractUserId(principal),
                CmmUtil.nvl(body.get("articleUrl"))
        );

        return ResponseEntity.ok(Map.of("liked", liked));
    }
}
