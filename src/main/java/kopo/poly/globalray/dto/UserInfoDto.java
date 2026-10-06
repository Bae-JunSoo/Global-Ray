package kopo.poly.globalray.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import java.time.LocalDateTime;

// 회원 정보 전달용 DTO: @Setter 없이 @Builder로만 만들어 생성 후 값이 바뀌지 않게 함
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