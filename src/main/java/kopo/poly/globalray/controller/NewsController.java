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

/**
 * [변경 사항 1 - Repository 직접 주입 제거]
 * 이전: NewsArticleRepository 를 Controller 에 직접 주입
 *       → Controller → Service → Repository 계층 구조 위반
 *       → Controller 가 DB 접근 방식을 직접 알게 되어 관심사 분리 실패
 * 이후: Repository 를 Service 뒤로 완전히 숨김, Controller 는 Service 만 의존
 *
 * [변경 사항 2 - 비즈니스 로직 Controller 제거]
 * 이전: on-demand 심화요약 생성 로직(Gemini 호출 + DB 저장)이 newsDetail() 안에 있었음
 *       → 언제 요약을 생성할지, 어떻게 저장할지 등 비즈니스 결정이 Controller 에 있으면 안 됨
 * 이후: NewsServiceImpl.getArticleById() 안으로 이동
 *       Controller 는 결과를 받아 View 에 전달하는 역할만 담당
 *
 * [변경 사항 3 - IGeminiService 제거]
 * on-demand 요약 로직이 Service 로 이동함에 따라 Controller 에서 의존 불필요
 *
 * [변경 사항 4 - extractUserId 호출 방식]
 * 이전: GlobalControllerAdvice.extractUserId() (강결합)
 * 이후: SecurityUtil.extractUserId() (유틸 클래스 의존)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class NewsController {

    private final INewsService newsService;
    private final IBookmarkService bookmarkService;

    // 카테고리별 뉴스 목록
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

    /**
     * 뉴스 상세 페이지
     * on-demand 심화요약 생성 로직은 NewsServiceImpl.getArticleById() 내부에서 처리
     * Controller 는 단순히 Service 결과를 View 에 전달만 함
     */
    @GetMapping("/news/detail/{articleId}")
    public String newsDetail(@PathVariable String articleId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             @AuthenticationPrincipal OAuth2User oAuth2User,
                             Model model) {

        String userId = SecurityUtil.extractUserId(userDetails, oAuth2User);

        // 조회수 +1 (상세 페이지 진입 시점에 카운트 — 봇/크롤러 필터링은 향후 개선 여지)
        newsService.increaseViewCount(CmmUtil.nvl(articleId));

        // 요약이 없으면 Service 내부에서 자동 생성 후 반환
        NewsDto article = newsService.getArticleById(CmmUtil.nvl(articleId), userId);

        model.addAttribute("article", article);

        return "news/detail";
    }

    // 북마크 토글 (Ajax)
    @PostMapping("/bookmark/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleBookmark(
            @RequestBody Map<String, String> body,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        String userId = SecurityUtil.extractUserId(principal);

        boolean added = bookmarkService.toggleBookmark(
                userId,
                CmmUtil.nvl(body.get("articleUrl"))
        );

        return ResponseEntity.ok(Map.of("bookmarked", added));
    }
}
