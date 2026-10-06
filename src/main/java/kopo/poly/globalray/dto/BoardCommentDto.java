package kopo.poly.globalray.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardCommentDto {
    private Long id;
    private String content;
    private LocalDateTime regDt;
    private LocalDateTime modDt;
    private boolean mine;
}
