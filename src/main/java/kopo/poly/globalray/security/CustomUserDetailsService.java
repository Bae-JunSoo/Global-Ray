package kopo.poly.globalray.security;

import kopo.poly.globalray.entity.UserInfoEntity;
import kopo.poly.globalray.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserInfoRepository userInfoRepository;

    @org.springframework.beans.factory.annotation.Value("${app.admin-email}")
    private String adminEmail;

    /**
     * Spring Security 로그인 처리 시 호출 → DB 에서 사용자 조회
     * 비밀번호 비교는 SecurityConfig 의 DaoAuthenticationProvider + BCryptPasswordEncoder 가 처리
     *
     * [변경 사항]
     * 기존: User.builder() 로 기본 UserDetails 반환 (userName 포함 안 됨)
     * 변경: CustomUserDetails 반환 → userName 을 세션에 함께 저장
     *       → GlobalControllerAdvice 에서 매 요청마다 DB 조회하던 문제 해결
     */
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UserInfoEntity user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다 : " + userId));

        log.info("로그인 시도 userId : {}", userId);

        String role = adminEmail.equalsIgnoreCase(user.getUserEmail()) ? "ROLE_ADMIN" : "ROLE_USER";

        return new CustomUserDetails(
                user.getUserId(),
                user.getUserPw(),
                List.of(new SimpleGrantedAuthority(role)),
                user.getUserName()
        );
    }
}