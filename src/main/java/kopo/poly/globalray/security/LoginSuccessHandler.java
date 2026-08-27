package kopo.poly.globalray.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kopo.poly.globalray.entity.LoginHistoryEntity;
import kopo.poly.globalray.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginHistoryRepository loginHistoryRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String userId = "";
        String userName = "";
        String loginType = "NORMAL";

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails ud) {
            userId = ud.getUsername();
            userName = ud.getUserName();
        } else if (principal instanceof OAuth2User ou) {
            Object customId = ou.getAttribute("customUserId");
            userId = customId != null ? customId.toString() : "";
            Object nameAttr = ou.getAttribute("name");
            userName = nameAttr != null ? nameAttr.toString() : "";
            loginType = "GOOGLE";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }

        loginHistoryRepository.save(LoginHistoryEntity.builder()
                .userId(userId)
                .userName(userName)
                .ipAddress(ip)
                .loginType(loginType)
                .build());

        log.info("로그인 이력 저장: {} ({})", userId, loginType);
        response.sendRedirect("/main");
    }
}
