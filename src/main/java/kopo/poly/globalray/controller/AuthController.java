package kopo.poly.globalray.controller;

import jakarta.validation.Valid;
import kopo.poly.globalray.dto.SignupRequest;
import kopo.poly.globalray.dto.UserInfoDto;
import kopo.poly.globalray.service.IUserInfoService;
import kopo.poly.globalray.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IUserInfoService userInfoService;

    // 로그인 페이지
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String oauth2error,
            Model model) {
        if (error != null) {
            model.addAttribute("errorMsg", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        if (oauth2error != null) {
            model.addAttribute("errorMsg", "Google 로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
        return "auth/login";
    }

    // 회원가입 페이지
    @GetMapping("/signup")
    public String signupPage() {
        return "auth/signup";
    }

    // BindingResult를 받으면 검증 실패 시 예외 대신 오류 정보가 담겨 와서, 같은 화면에 메시지를 보여줄 수 있음
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute SignupRequest request,
                         BindingResult bindingResult,
                         Model model) {

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            model.addAttribute("errorMsg", errorMsg);
            return "auth/signup";
        }

        try {
            // 서비스가 화면용 요청 DTO(SignupRequest)에 의존하지 않도록 변환해서 전달
            UserInfoDto dto = UserInfoDto.builder()
                    .userId(request.getUserId())
                    .userPw(request.getUserPw())
                    .userName(request.getUserName())
                    .userEmail(request.getUserEmail())
                    .build();

            userInfoService.signup(dto);
            return "redirect:/auth/login?signup=success";

        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/signup";
        }
    }

    // OAuth2 중복 콜백 처리 후 로그인 완료 여부 폴링용 API
    @GetMapping("/api/login-check")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> loginCheck(Authentication authentication) {
        boolean loggedIn = authentication != null && authentication.isAuthenticated();
        return ResponseEntity.ok(Map.of("loggedIn", loggedIn));
    }

    // 아이디 중복 확인 (Ajax)
    @GetMapping("/api/check-id")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkId(@RequestParam String userId) {
        boolean duplicate = userInfoService.isUserIdDuplicate(CmmUtil.nvl(userId));
        return ResponseEntity.ok(Map.of("duplicate", duplicate));
    }

    // 이메일 인증코드 발송 (Ajax)
    @PostMapping("/api/send-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sendCode(@RequestBody Map<String, String> body) {
        try {
            userInfoService.sendEmailAuthCode(CmmUtil.nvl(body.get("email")));
            return ResponseEntity.ok(Map.of("message", "인증코드가 발송되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "발송 실패 : " + e.getMessage()));
        }
    }

    // 이메일 인증코드 검증 (Ajax)
    @PostMapping("/api/verify-code")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> verifyCode(@RequestBody Map<String, String> body) {
        boolean verified = userInfoService.verifyEmailCode(
                CmmUtil.nvl(body.get("email")),
                CmmUtil.nvl(body.get("code"))
        );
        return ResponseEntity.ok(Map.of("verified", verified));
    }

    // 아이디 찾기 페이지
    @GetMapping("/find-id")
    public String findIdPage() {
        return "auth/find-id";
    }

    // 아이디 찾기 처리
    @PostMapping("/find-id")
    public String findId(@RequestParam String userName,
                         @RequestParam String userEmail,
                         Model model) {
        String userId = userInfoService.findUserId(
                CmmUtil.nvl(userName),
                CmmUtil.nvl(userEmail)
        );
        if (userId != null) {
            model.addAttribute("foundId", userId);
        } else {
            model.addAttribute("errorMsg", "일치하는 회원 정보가 없습니다.");
        }
        return "auth/find-id";
    }

    // 비밀번호 찾기 페이지
    @GetMapping("/find-pw")
    public String findPwPage() {
        return "auth/find-pw";
    }

    // 비밀번호 찾기 처리
    @PostMapping("/find-pw")
    public String findPw(@RequestParam String userId,
                         @RequestParam String userEmail,
                         Model model) {
        try {
            boolean result = userInfoService.resetPassword(
                    CmmUtil.nvl(userId),
                    CmmUtil.nvl(userEmail)
            );
            if (result) {
                model.addAttribute("successMsg", "임시 비밀번호가 이메일로 발송되었습니다.");
            } else {
                model.addAttribute("errorMsg", "일치하는 회원 정보가 없습니다.");
            }
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
        }
        return "auth/find-pw";
    }
}
