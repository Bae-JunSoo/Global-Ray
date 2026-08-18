package kopo.poly.globalray.controller;

import kopo.poly.globalray.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * 모든 컨트롤러에 공통으로 적용되는 전역 설정
 *
 * [변경 사항 1 - @ModelAttribute("userName") DB 조회 제거]
 * 이전: 모든 요청마다 userInfoService.getUserInfo(userId) 로 DB SELECT 실행
 *       → 사용자가 페이지 이동할 때마다 불필요한 DB 쿼리 발생
 * 이후: CustomUserDetails 에 userName 이 이미 담겨 있으므로
 *       세션에서 바로 꺼내기만 하면 됨 → DB 조회 완전 제거
 *
 * [변경 사항 2 - extractUserId static 메서드 제거]
 * 이전: GlobalControllerAdvice.extractUserId() 로 컨트롤러에서 직접 참조
 *       → @ControllerAdvice 에 유틸 로직이 섞이는 단일 책임 위반
 *       → 컨트롤러가 GlobalControllerAdvice 클래스에 강결합
 * 이후: SecurityUtil.extractUserId() 로 이동 (별도 유틸 클래스)
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    // 모든 View 에 공통으로 전달할 카테고리 목록
    @ModelAttribute("categories")
    public List<String> categories() {
        return List.of("경제", "엔터테인먼트", "종합", "보건", "과학", "IT", "스포츠");
    }

    /**
     * 모든 View 에 로그인 사용자 이름을 전달
     *
     * - 일반 로그인: CustomUserDetails.getUserName() → 로그인 시 세션에 저장된 값 사용 (DB 조회 없음)
     * - Google 로그인: OAuth2User attributes["name"] → OAuth2 인증 시 받아온 값 사용 (DB 조회 없음)
     */
    @ModelAttribute("userName")
    public String userName(@AuthenticationPrincipal Object principal) {
        if (principal instanceof CustomUserDetails customUser) {
            // 로그인 시 loadUserByUsername() 에서 이미 userName 을 담아뒀으므로 DB 조회 불필요
            return customUser.getUserName();
        }
        if (principal instanceof OAuth2User oAuth2User) {
            return (String) oAuth2User.getAttributes().get("name");
        }
        return null;
    }
}
