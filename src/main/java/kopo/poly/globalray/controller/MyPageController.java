package kopo.poly.globalray.controller;

import kopo.poly.globalray.dto.NewsDto;
import kopo.poly.globalray.dto.UserInfoDto;
import kopo.poly.globalray.service.INewsService;
import kopo.poly.globalray.service.IUserInfoService;
import kopo.poly.globalray.util.CmmUtil;
import kopo.poly.globalray.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * [변경 사항 - extractUserId 호출 방식]
 * 이전: GlobalControllerAdvice.extractUserId(principal) (강결합)
 * 이후: SecurityUtil.extractUserId(principal) (유틸 클래스 의존)
 */
@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final IUserInfoService userInfoService;
    private final INewsService newsService;

    @GetMapping
    public String myPage(Principal principal, Model model) {
        String userId = SecurityUtil.extractUserId(principal);
        log.info("마이페이지 접근 userId : {}", userId);

        UserInfoDto user = userInfoService.getUserInfo(userId);
        model.addAttribute("user", user);

        List<NewsDto> bookmarks = newsService.getBookmarkedNews(userId);

        List<NewsDto> recentBookmarks = bookmarks.stream()
                .limit(3)
                .collect(Collectors.toList());
        model.addAttribute("recentBookmarks", recentBookmarks);
        model.addAttribute("bookmarkCount", bookmarks.size());

        long categoryCount = bookmarks.stream()
                .map(NewsDto::getCatType)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .count();
        model.addAttribute("categoryCount", categoryCount);

        if (user.getRegDt() != null) {
            long joinDays = ChronoUnit.DAYS.between(user.getRegDt().toLocalDate(), LocalDate.now());
            model.addAttribute("joinDays", joinDays + "일");
        } else {
            model.addAttribute("joinDays", "-");
        }

        return "mypage/index";
    }

    @GetMapping("/bookmark")
    public String bookmarkList(Principal principal, Model model) {
        String userId = SecurityUtil.extractUserId(principal);
        List<NewsDto> bookmarks = newsService.getBookmarkedNews(userId);
        model.addAttribute("bookmarks", bookmarks);
        return "mypage/bookmark";
    }

    @GetMapping("/change-pw")
    public String changePwPage(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken) {
            return "redirect:/mypage";
        }
        return "mypage/change-pw";
    }

    @PostMapping("/change-pw")
    public String changePw(@RequestParam String currentPw,
                           @RequestParam String newPw,
                           @RequestParam String confirmPw,
                           Principal principal,
                           Model model) {
        if (principal instanceof OAuth2AuthenticationToken) {
            return "redirect:/mypage";
        }
        try {
            if (!newPw.equals(confirmPw)) {
                model.addAttribute("errorMsg", "새 비밀번호가 일치하지 않습니다.");
                return "mypage/change-pw";
            }
            boolean result = userInfoService.changePassword(
                    SecurityUtil.extractUserId(principal),
                    CmmUtil.nvl(currentPw),
                    CmmUtil.nvl(newPw)
            );
            if (result) {
                return "redirect:/mypage?pwChanged=true";
            } else {
                model.addAttribute("errorMsg", "현재 비밀번호가 올바르지 않습니다.");
                return "mypage/change-pw";
            }
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "mypage/change-pw";
        }
    }

    @PostMapping("/delete")
    public String deleteUser(Principal principal,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        userInfoService.deleteUser(SecurityUtil.extractUserId(principal));
        new SecurityContextLogoutHandler().logout(request, response,
                SecurityContextHolder.getContext().getAuthentication());
        return "redirect:/main";
    }
}
