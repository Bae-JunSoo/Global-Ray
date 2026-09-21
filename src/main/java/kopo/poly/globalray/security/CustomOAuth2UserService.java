package kopo.poly.globalray.security;

import kopo.poly.globalray.dto.UserInfoDto;
import kopo.poly.globalray.service.IUserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final IUserInfoService userInfoService;

    @Value("${app.admin-email}")
    private String adminEmail;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name  = (String) attributes.get("name");
        String sub   = (String) attributes.get("sub");

        String newUserId = "GOOGLE_" + sub;

        // Repository 직접 접근 제거 → IUserInfoService 경유 (@Transactional 적용됨)
        UserInfoDto user = userInfoService.findOrCreateOAuth2User(newUserId, email, name, "GOOGLE");

        log.info("Google 로그인 유저 : {} (userId: {})", email, user.getUserId());

        // attributes 에 실제 DB userId 를 추가
        // SecurityUtil.extractUserId() 가 이 값을 우선 사용 → DB 형식과 항상 일치
        Map<String, Object> enrichedAttributes = new HashMap<>(attributes);
        enrichedAttributes.put("customUserId", user.getUserId());

        String role = adminEmail.equalsIgnoreCase(email) ? "ROLE_ADMIN" : "ROLE_USER";

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(role)),
                enrichedAttributes,
                "name"
        );
    }
}
