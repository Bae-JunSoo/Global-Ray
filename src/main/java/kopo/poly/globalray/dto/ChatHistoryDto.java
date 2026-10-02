package kopo.poly.globalray.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatHistoryDto {
    private Long chatId;
    private String question;
    private String answer;
    private int sourceCount;
    private LocalDateTime regDt;
}
