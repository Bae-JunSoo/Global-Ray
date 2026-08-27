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

/**
 * [변경 사항 - 미사용 의존성 제거]
 * 이전: IUserInfoService 를 주입했지만 이 컨트롤러 어디에서도 사용하지 않았음
 *       → 불필요한 의존성은 코드 가독성을 낮추고 테스트 시 불필요한 Mock 을 요구함
 * 이후: IUserInfoService 제거, INewsService 만 유지
 *
 * [변경 사항 - extractUserId 호출 방식]
 * 이전: GlobalControllerAdvice.extractUserId(userDetails, oAuth2User) (강결합)
 * 이후: SecurityUtil.extractUserId(userDetails, oAuth2User) (유틸 클래스 의존)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    private final INewsService newsService;

    @GetMapping({"/", "/main"})
    public String main(@RequestParam(required = false, defaultValue = "") String cat,
                       @RequestParam(required = false, defaultValue = "0") int page,
                       @AuthenticationPrincipal UserDetails userDetails,
                       @AuthenticationPrincipal OAuth2User oAuth2User,
                       Model model) {

        String userId = SecurityUtil.extractUserId(userDetails, oAuth2User);

        Page<NewsDto> newsPage;
        if (cat.isBlank()) {
            newsPage = newsService.getMainNews(page, userId);
        } else {
            newsPage = newsService.getNewsByCategory(cat, page, userId);
        }

        int pageGroupStart = (page / 10) * 10;
        int pageGroupEnd = Math.min(pageGroupStart + 10, newsPage.getTotalPages());

        model.addAttribute("newsList", newsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", newsPage.getTotalPages());
        model.addAttribute("currentCat", cat);
        model.addAttribute("userId", userId);
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
