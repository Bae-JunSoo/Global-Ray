package kopo.poly.globalray.util;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.security.Principal;

// 로그인 방식(일반 / Google)에 관계없이 DB의 userId를 꺼내는 유틸
public class SecurityUtil {

    private SecurityUtil() {}

    // 비로그인 요청이면 Spring MVC가 Principal을 null로 넘기므로 null 반환
    public static String extractUserId(Principal principal) {
        if (principal == null) return null;

        if (principal instanceof OAuth2AuthenticationToken oauthToken) {
            // CustomOAuth2UserService가 attributes에 넣어둔 실제 DB userId를 우선 사용
            String customUserId = (String) oauthToken.getPrincipal().getAttributes().get("customUserId");
            if (customUserId != null) return customUserId;

            String sub = (String) oauthToken.getPrincipal().getAttributes().get("sub");
            return "GOOGLE_" + sub;
        }

        // 일반 로그인은 Principal 이름이 곧 userId
        return principal.getName();
    }
}
