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
                           @RequestParam(required = false, defaultValue = "0") int page,
                           @RequestParam(required = false, defaultValue = "ALL") String country,
                           @AuthenticationPrincipal UserDetails userDetails,
                           @AuthenticationPrincipal OAuth2User oAuth2User,
                           Model model) {
        String userId = SecurityUtil.extractUserId(userDetails, oAuth2User);
        boolean filtered = country != null && !country.isBlank() && !"ALL".equals(country);

        org.springframework.data.domain.Page<NewsDto> newsPage = filtered
                ? newsService.getNewsByCategory(CmmUtil.nvl(catType), page, userId, country)
                : newsService.getNewsByCategory(CmmUtil.nvl(catType), page, userId);

        int pageGroupStart = (page / 10) * 10;
        int pageGroupEnd = Math.min(pageGroupStart + 10, newsPage.getTotalPages());

        model.addAttribute("newsList", newsPage.getContent());
        model.addAttribute("catType", catType);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", newsPage.getTotalPages());
        model.addAttribute("currentCountry", country);
        model.addAttribute("pageGroupStart", pageGroupStart);
        model.addAttribute("pageGroupEnd", pageGroupEnd);
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
