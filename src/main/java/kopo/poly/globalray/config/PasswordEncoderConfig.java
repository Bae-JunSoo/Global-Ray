package kopo.poly.globalray.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// SecurityConfig에서 분리 — SecurityConfig가 IUserInfoService를 간접 참조할 때
// PasswordEncoder가 SecurityConfig 안에 있으면 순환 의존성이 발생하므로 별도 클래스로 분리
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
