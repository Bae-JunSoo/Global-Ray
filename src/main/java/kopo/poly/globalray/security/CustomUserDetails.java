package kopo.poly.globalray.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Spring Security 기본 User 를 확장해서 userName(실명)을 함께 보관하는 클래스
 *
 * [이전 문제]
 * GlobalControllerAdvice 의 @ModelAttribute("userName") 이
 * 모든 HTTP 요청마다 DB SELECT 를 실행해서 userName 을 가져왔음
 *
 *   @ModelAttribute("userName")
 *   public String userName(...) {
 *       UserInfoDto userInfo = userInfoService.getUserInfo(userId); // 매 요청마다 DB 조회
 *   }
 *
 * [해결]
 * 로그인 시점(CustomUserDetailsService.loadUserByUsername)에 userName 을 CustomUserDetails 에 담음
 * 이후 모든 요청에서 DB 조회 없이 Principal 에서 바로 꺼낼 수 있음
 * → 세션 유지 기간 동안 userName DB 조회가 단 1회로 줄어듦
 */
public class CustomUserDetails extends User {

    // 직렬화(세션 저장) 시 클래스 버전 불일치 경고 방지
    private static final long serialVersionUID = 1L;

    // 화면에 표시할 실명 (로그인 시 1회 로딩, 이후 세션에서 재사용)
    private final String userName;

    public CustomUserDetails(String userId, String userPw,
                             Collection<? extends GrantedAuthority> authorities,
                             String userName) {
        super(userId, userPw, authorities);
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }
}
