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

    /**
     * 회원가입 처리
     *
     * [변경 사항 1 - @ModelAttribute 타입 변경]
     * 이전: UserInfoDto (응답 DTO를 요청에 혼용, @Valid 없음)
     * 이후: SignupRequest (요청 전용 DTO, Bean Validation 어노테이션 적용)
     *
     * [변경 사항 2 - @Valid + BindingResult 추가]
     * @Valid: SignupRequest 의 @NotBlank, @Size, @Email 검증을 자동 실행
     * BindingResult: 검증 실패 시 예외를 던지지 않고 오류 내용을 담아 컨트롤러로 돌아옴
     * → 빈 아이디/비밀번호, 잘못된 이메일 형식 등을 서버에서 즉시 차단
     *
     * [컨트롤러→서비스 변환]
     * SignupRequest 를 UserInfoDto 로 변환 후 서비스에 전달
     * → 서비스 레이어는 표현 계층(SignupRequest) 과 무관하게 유지
     */
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute SignupRequest request,
                         BindingResult bindingResult,
                         Model model) {

        // Bean Validation 실패 시 첫 번째 오류 메시지를 화면에 전달
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            model.addAttribute("errorMsg", errorMsg);
            return "auth/signup";
        }

        try {
            // SignupRequest → UserInfoDto 변환 (서비스는 표현 계층 DTO 에 의존하지 않음)
            UserInfoDto dto = UserInfoDto.builder() // builder = 밑에 있는 값들을 하나로 모아서 하나의 객체로 묶음
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
