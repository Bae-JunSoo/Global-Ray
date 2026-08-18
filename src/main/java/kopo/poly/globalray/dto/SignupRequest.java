package kopo.poly.globalray.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원가입 폼 바인딩 전용 요청 DTO
 *
 * [이전 문제 1 - 요청/응답 DTO 미분리]
 * UserInfoDto 를 회원가입 입력과 회원 정보 응답 양쪽에 혼용했음
 * → 응답 전용 필드(regDt 등)가 요청 DTO 에 섞이고
 * → @Setter 가 userPw 를 포함한 모든 필드에 적용되는 문제 발생
 *
 * [이전 문제 2 - 입력값 검증 없음]
 * userId, userPw, userEmail 에 아무 제약이 없어서
 * 빈 문자열이나 형식이 맞지 않는 값으로도 가입 요청이 서비스까지 그대로 내려갔음
 *
 * [해결]
 * 요청 전용 SignupRequest 를 분리하고 Bean Validation 어노테이션 추가
 * → @Valid 와 함께 컨트롤러에서 요청 시점에 즉시 검증
 * → UserInfoDto 는 응답 전용으로만 사용 (@Setter 제거 가능)
 *
 * [@Setter 를 요청 DTO 에만 허용하는 이유]
 * Spring MVC 의 @ModelAttribute 는 기본 생성자 + setter 방식으로 폼 데이터를 바인딩함
 * 응답 DTO(UserInfoDto)는 @Builder 로만 생성하므로 @Setter 불필요
 */
@Getter
@Setter         // @ModelAttribute 폼 바인딩을 위해 필요 (Spring MVC 가 setter 로 필드를 채움)
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    @Size(min = 4, max = 20, message = "아이디는 4~20자로 입력해주세요.")
    private String userId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 8자 이상 입력해주세요.")
    private String userPw;

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 50, message = "이름은 50자 이내로 입력해주세요.")
    private String userName;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String userEmail;
}
