package kopo.poly.globalray.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EmailAuthDto {

    private Long authId;
    private String reqEmail;
    private String authCode;
    private Integer isVerified;
    private LocalDateTime expireDt;
}