package kopo.poly.globalray.config;

import kopo.poly.globalray.security.CustomOAuth2UserService;
import kopo.poly.globalray.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService oAuth2UserService;
    private final ClientRegistrationRepository clientRegistrationRepository;

    // - SHA-256은 연산 속도가 빠르기 때문에 brute-force/레인보우 테이블 공격에 취약
    // - BCrypt는 의도적으로 연산 비용이 높고, 호출마다 자동으로 랜덤 salt를 생성하여 저장
    //   → 동일한 비밀번호라도 매번 다른 해시값이 나와 레인보우 테이블 공격 원천 차단
    // - strength 기본값(10): 2^10 = 1024번 반복 해시 → 현업 권장 수준
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Spring Security 표준 AuthenticationProvider
    // - CustomUserDetailsService.loadUserByUsername()으로 DB 조회
    // - passwordEncoder().matches()로 비밀번호 비교
    // - 위 두 단계는 Spring Security가 자동으로 처리하므로 별도 구현 불필요
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // [수정] Spring Security 7부터 기본 생성자 + setUserDetailsService()가 제거됨
        //        -> 생성자에서 UserDetailsService를 직접 주입받는 방식으로 변경
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // authorization request(state) 저장소를 명시적으로 선언하여
    // OAuth2AuthorizationRequestRedirectFilter와 OAuth2LoginAuthenticationFilter가
    // 동일 인스턴스를 공유함을 보장 → state mismatch 방지
    @Bean
    public HttpSessionOAuth2AuthorizationRequestRepository authorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    // 구글 로그인 시 매번 계정 선택창이 뜨도록 prompt=select_account 추가
    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver() {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(customizer ->
                customizer.additionalParameters(params ->
                        params.put("prompt", "select_account")
                )
        );
        return resolver;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 보호 활성화 (세션 기반이므로 반드시 필요)
                // CookieCsrfTokenRepository: JS에서 XSRF-TOKEN 쿠키를 읽어 X-XSRF-TOKEN 헤더로 전송
                // CsrfTokenRequestAttributeHandler: XOR 마스킹 없이 원본 토큰 사용 → 쿠키값과 일치
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                // [수정] customAuthenticationProvider() (SHA256 비교+예외처리 직접 구현) 삭제
                //        -> authenticationProvider() (DaoAuthenticationProvider, Spring 표준)로 교체
                .authenticationProvider(authenticationProvider())
                // 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/main/**", "/news/**",
                                "/auth/**", "/css/**", "/js/**", "/images/**"
                        ).permitAll()
                        .requestMatchers(
                                "/mypage/**", "/bookmark/**", "/chatbot/**"
                        ).authenticated()
                        .anyRequest().authenticated()
                )
                // 일반 로그인 설정
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login") // 이 URL의 POST 요청을 Spring Security가 처리
                        .usernameParameter("userId")       // 폼 필드명 매핑
                        .passwordParameter("userPw")
                        .defaultSuccessUrl("/main", true) //성공시 main let's go
                        .failureUrl("/auth/login?error=true")
                        .permitAll()
                )
                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/main")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // Google OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/auth/login")
                        .userInfoEndpoint(info -> info.userService(oAuth2UserService))
                        .defaultSuccessUrl("/main", true)
                        // OAuth2 실패 핸들러
                        .failureHandler((request, response, exception) -> {
                            boolean isStateConsumed =
                                    exception instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException oae
                                    && "authorization_request_not_found".equals(oae.getError().getErrorCode());

                            if (isStateConsumed) {
                                // 브라우저가 OAuth2 콜백 URL을 동시에 두 번 요청하는 현상
                                // exec-A(성공): state 소진 → 새 세션 발급(Set-Cookie) → 302 /main
                                // exec-B(실패): state 없음 → 이 핸들러 진입
                                // exec-B가 즉시 302 /main 하면 브라우저가 Set-Cookie 받기 전에 구 세션으로 /main 접근 → 비인증
                                // Thread.sleep은 서버 스레드를 블로킹하여 타이밍이 불안정하므로,
                                // 클라이언트 측 meta refresh로 1초 지연 → 브라우저가 Set-Cookie를 먼저 처리한 뒤 /main 이동
                                log.warn("OAuth2 중복 콜백 감지: 클라이언트 1초 대기 후 /main 이동");
                                response.setContentType("text/html;charset=UTF-8");
                                response.setStatus(200);
                                response.getWriter().write(
                                        "<html><head>" +
                                        "<meta http-equiv='refresh' content='1;url=/main'>" +
                                        "</head><body></body></html>"
                                );
                                return;
                            }

                            log.error("OAuth2 로그인 실패 [{}]: {}", exception.getClass().getSimpleName(), exception.getMessage());
                            response.sendRedirect("/auth/login?oauth2error=true");
                        })
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(authorizationRequestResolver())
                                // 명시적으로 동일 repository 인스턴스 지정
                                .authorizationRequestRepository(authorizationRequestRepository())
                        )
                );

        return http.build();
    }
}