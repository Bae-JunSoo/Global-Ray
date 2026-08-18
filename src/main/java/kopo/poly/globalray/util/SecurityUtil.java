package kopo.poly.globalray.util;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.security.Principal;

/**
 * 로그인 방식(일반 / Google OAuth2)에 따라 userId를 추출하는 보안 유틸리티
 */
public class SecurityUtil {

    private SecurityUtil() {}

    /**
     * Principal → userId 추출
     *
     * - 일반 로그인: principal.getName() = userId (Spring Security 기본 동작)
     * - Google 로그인: attributes["customUserId"] 우선 사용
     *   → CustomOAuth2UserService 에서 실제 DB userId 를 enrichedAttributes 에 담아 전달
     *   → DB 형식(GOOGLE_sub 또는 구형 GOOGLE_이메일앞부분)과 항상 일치
     *   → fallback: GOOGLE_ + sub (customUserId 없는 경우 대비)
     */
    public static String extractUserId(Principal principal) {
        if (principal == null) return null;

        if (principal instanceof OAuth2AuthenticationToken oauthToken) {
            // CustomOAuth2UserService 가 실제 DB userId 를 enrichedAttributes 에 저장
            String customUserId = (String) oauthToken.getPrincipal().getAttributes().get("customUserId");
            if (customUserId != null) return customUserId;

            // fallback: 신규 유저는 GOOGLE_sub 로 저장됨
            String sub = (String) oauthToken.getPrincipal().getAttributes().get("sub");
            return "GOOGLE_" + sub;
        }

        return principal.getName();
    }

    /**
     * @AuthenticationPrincipal 로 UserDetails / OAuth2User 를 각각 주입받은 경우 사용
     * (MainController, NewsController 등)
     */
    public static String extractUserId(UserDetails userDetails, OAuth2User oAuth2User) {
        if (userDetails != null) return userDetails.getUsername();

        if (oAuth2User != null) {
            // CustomOAuth2UserService 가 enrichedAttributes 에 넣은 실제 DB userId 우선 사용
            String customUserId = (String) oAuth2User.getAttributes().get("customUserId");
            if (customUserId != null) return customUserId;

            String sub = (String) oAuth2User.getAttributes().get("sub");
            return "GOOGLE_" + sub;
        }

        return null;
    }
}
