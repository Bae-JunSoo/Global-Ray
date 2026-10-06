package kopo.poly.globalray.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

// 기본 User에 화면 표시용 이름을 추가로 보관 (로그인 때 한 번 담아두면 이후 요청에서 DB 조회 불필요)
public class CustomUserDetails extends User {

    // 세션에 직렬화되어 저장되므로 클래스 버전을 명시
    private static final long serialVersionUID = 1L;

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
