package kopo.poly.globalray.security;

import kopo.poly.globalray.entity.UserInfoEntity;
import kopo.poly.globalray.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserInfoRepository userInfoRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email  = (String) attributes.get("email");
        String name   = (String) attributes.get("name");
        String sub    = (String) attributes.get("sub");

        // 신규 가입 시 생성할 userId (GOOGLE_ + sub 형식)
        String newUserId = "GOOGLE_" + sub;

        // 이메일로 기존 유저 조회 → 없으면 신규 생성
        // 기존 유저의 경우 구형 userId (GOOGLE_이메일앞부분) 그대로 유지됨
        UserInfoEntity user = userInfoRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    UserInfoEntity newUser = UserInfoEntity.builder()
                            .userId(newUserId)
                            .userPw("")
                            .userName(name)
                            .userEmail(email)
                            .socialType("GOOGLE")
                            .build();
                    log.info("신규 Google 유저 등록 : {} → {}", email, newUserId);
                    return userInfoRepository.save(newUser);
                });

        log.info("Google 로그인 유저 : {} (userId: {})", email, user.getUserId());

        // attributes 에 실제 DB userId 를 추가
        // SecurityUtil.extractUserId() 가 이 값을 우선 사용 → DB 형식과 항상 일치
        // 배포 전 구형 userId (GOOGLE_이메일앞부분) 를 쓰던 기존 유저도 정상 동작
        Map<String, Object> enrichedAttributes = new HashMap<>(attributes);
        enrichedAttributes.put("customUserId", user.getUserId());

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                enrichedAttributes,
                "name"
        );
    }
}
