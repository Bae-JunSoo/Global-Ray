package kopo.poly.globalray.controller;

import kopo.poly.globalray.dto.NewsDto;
import kopo.poly.globalray.service.INewsService;
import kopo.poly.globalray.util.CmmUtil;
import kopo.poly.globalray.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    private final INewsService newsService;

    @GetMapping({"/", "/main"})
    public String main(@RequestParam(required = false, defaultValue = "") String cat,
                       @RequestParam(required = false, defaultValue = "0") int page,
                       @RequestParam(required = false, defaultValue = "ALL") String country,
                       @AuthenticationPrincipal UserDetails userDetails,
                       @AuthenticationPrincipal OAuth2User oAuth2User,
                       Model model) {

        String userId = SecurityUtil.extractUserId(userDetails, oAuth2User);
        boolean filtered = country != null && !country.isBlank() && !"ALL".equals(country);

        Page<NewsDto> newsPage;
        if (cat.isBlank()) {
            newsPage = filtered
                    ? newsService.getMainNews(page, userId, country)
                    : newsService.getMainNews(page, userId);
        } else {
            newsPage = filtered
                    ? newsService.getNewsByCategory(cat, page, userId, country)
                    : newsService.getNewsByCategory(cat, page, userId);
        }

        int pageGroupStart = (page / 10) * 10;
        int pageGroupEnd = Math.min(pageGroupStart + 10, newsPage.getTotalPages());

        model.addAttribute("newsList", newsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", newsPage.getTotalPages());
        model.addAttribute("currentCat", cat);
        model.addAttribute("currentCountry", country);
        model.addAttribute("pageGroupStart", pageGroupStart);
        model.addAttribute("pageGroupEnd", pageGroupEnd);
        return "main/index";
    }

    @GetMapping("/main/search")
    public String search(@RequestParam String keyword,
                         @AuthenticationPrincipal UserDetails userDetails,
                         @AuthenticationPrincipal OAuth2User oAuth2User,
                         Model model) {

        String userId = SecurityUtil.extractUserId(userDetails, oAuth2User);

        List<NewsDto> newsList = newsService.searchNews(CmmUtil.nvl(keyword), userId);

        model.addAttribute("newsList", newsList);
        model.addAttribute("keyword", keyword);

        return "main/search";
    }
}
