package kopo.poly.globalray.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LoginHistoryDto {
    private String userId;
    private String userName;
    private String loginType;
    private String ipAddress;
    private LocalDateTime loginDt;
}
