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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final IUserInfoService userInfoService;

    @Value("${app.admin-email}")
    private String adminEmail;

    // 사용자 조회만 담당하고, 비밀번호 비교는 DaoAuthenticationProvider가 BCrypt로 처리
    @Override
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
