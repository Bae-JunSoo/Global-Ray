package kopo.poly.globalray.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 회원 정보 응답 전용 DTO
 *
 * [이전 문제]
 * @Setter 가 전체 적용되어 userPw 를 포함한 모든 필드가 외부에서 임의 변경 가능한 상태였음
 * 요청/응답 DTO 를 혼용해서 불필요한 필드(authCode 등)가 섞여 있었음
 *
 * [해결]
 * 응답 전용 DTO 로 역할을 명확히 하고 @Setter 제거
 * 값 설정은 @Builder 로만 가능하게 해서 불변성 보장
 * 회원가입 요청은 SignupRequest (요청 전용 DTO) 로 분리
 */
@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserInfoDto {

    private String userId;
    @JsonIgnore
    private String userPw;      // 서비스 내부 전달용 (응답 시 노출 금지)
    private String userName;
    private String userEmail;
    private String socialType;
    private LocalDateTime regDt;
}