package kopo.poly.globalray.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatSourceDto {
    private String articleId;
    private String title;
    private String sourceName;
}
