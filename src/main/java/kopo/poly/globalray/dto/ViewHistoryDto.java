package kopo.poly.globalray.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ViewHistoryDto {
    private String userId;
    private String title;
    private LocalDateTime viewDt;
}
