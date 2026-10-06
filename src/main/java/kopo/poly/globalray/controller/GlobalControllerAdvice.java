package kopo.poly.globalray.controller;

import kopo.poly.globalray.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

// 모든 화면에 공통으로 필요한 값(카테고리 목록, 로그인 사용자 이름)을 모델에 넣어줌
@ControllerAdvice
public class GlobalControllerAdvice {

    // 모든 View 에 공통으로 전달할 카테고리 목록
    @ModelAttribute("categories")
    public List<String> categories() {
        return List.of("경제", "엔터테인먼트", "종합", "보건", "과학", "IT", "스포츠");
    }

    // 매 요청마다 DB를 조회하지 않도록, 로그인 시점에 세션에 담아둔 이름을 그대로 사용
    @ModelAttribute("userName")
    public String userName(@AuthenticationPrincipal Object principal) {
        if (principal instanceof CustomUserDetails customUser) {
            return customUser.getUserName();
        }
        if (principal instanceof OAuth2User oAuth2User) {
            return (String) oAuth2User.getAttributes().get("name");
        }
        return null;
    }
}
