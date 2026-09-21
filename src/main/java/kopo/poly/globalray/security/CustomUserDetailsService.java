package kopo.poly.globalray.security;

import kopo.poly.globalray.dto.UserInfoDto;
import kopo.poly.globalray.service.IUserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final IUserInfoService userInfoService;

    @Value("${app.admin-email}")
    private String adminEmail;

    /**
     * Spring Security 로그인 처리 시 호출 → Service를 경유하여 DB 조회
     * 비밀번호 비교는 SecurityConfig 의 DaoAuthenticationProvider + BCryptPasswordEncoder 가 처리
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UserInfoDto user = userInfoService.getUserInfoForAuth(userId);

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
