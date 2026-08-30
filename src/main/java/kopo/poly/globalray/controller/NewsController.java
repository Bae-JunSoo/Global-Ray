package kopo.poly.globalray.controller;

import kopo.poly.globalray.dto.NewsDto;
import kopo.poly.globalray.service.IBookmarkService;
import kopo.poly.globalray.service.INewsService;
import kopo.poly.globalray.util.CmmUtil;
import kopo.poly.globalray.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NewsController {

    private final INewsService newsService;
    private final IBookmarkService bookmarkService;

    @GetMapping("/news/top10")
    public String top10(Model model) {
        model.addAttribute("top10News", newsService.getTop10ByViewCount());
        return "news/top10";
    }

    @GetMapping("/news/{catType}")
    public String newsList(@PathVariable String catType,
                           @AuthenticationPrincipal UserDetails userDetails,
                           @AuthenticationPrincipal OAuth2User oAuth2User,
                           Model model) {
        String userId = SecurityUtil.extractUserId(userDetails, oAuth2User);
        List<NewsDto> newsList = newsService.getNewsByCategory(CmmUtil.nvl(catType), userId);
        model.addAttribute("newsList", newsList);
        model.addAttribute("catType", catType);
        return "news/list";
    }

    @GetMapping("/news/detail/{articleId}")
    public String newsDetail(@PathVariable String articleId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             @AuthenticationPrincipal OAuth2User oAuth2User,
                             Model model) {
        String userId = SecurityUtil.extractUserId(userDetails, oAuth2User);
        String cleanId = CmmUtil.nvl(articleId);

        newsService.increaseViewCount(cleanId);

        NewsDto article = newsService.getArticleById(cleanId, userId);

        if (article != null) {
            String title = article.getTitleKor() != null ? article.getTitleKor() : article.getTitle();
            newsService.saveViewHistory(userId, cleanId, title);
        }

        model.addAttribute("article", article);
        return "news/detail";
    }

    @PostMapping("/bookmark/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleBookmark(
            @RequestBody Map<String, String> body,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        boolean added = bookmarkService.toggleBookmark(
                SecurityUtil.extractUserId(principal),
                CmmUtil.nvl(body.get("articleUrl"))
        );
        return ResponseEntity.ok(Map.of("bookmarked", added));
    }
}
