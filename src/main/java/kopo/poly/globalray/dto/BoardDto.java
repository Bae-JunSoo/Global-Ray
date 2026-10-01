package kopo.poly.globalray.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BoardDto {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime regDt;
    private int viewCount;
    private int commentCount;
    private List<BoardCommentDto> comments;
}
