package kopo.poly.globalray.controller;

import kopo.poly.globalray.dto.NewsDto;
import kopo.poly.globalray.service.INewsService;
import kopo.poly.globalray.util.CmmUtil;
import kopo.poly.globalray.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
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
                       Principal principal,
                       Model model) {

        String userId = SecurityUtil.extractUserId(principal);
        Page<NewsDto> newsPage = newsService.getNewsList(cat, country, page, userId);

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
                         Principal principal,
                         Model model) {

        String userId = SecurityUtil.extractUserId(principal);
        List<NewsDto> newsList = newsService.searchNews(CmmUtil.nvl(keyword), userId);

        model.addAttribute("newsList", newsList);
        model.addAttribute("keyword", keyword);
        return "main/search";
    }
}
