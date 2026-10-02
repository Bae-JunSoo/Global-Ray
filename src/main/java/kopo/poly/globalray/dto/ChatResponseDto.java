package kopo.poly.globalray.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatResponseDto {
    private String answer;
    private List<String> keywords;
    private List<ChatSourceDto> sources;
}
