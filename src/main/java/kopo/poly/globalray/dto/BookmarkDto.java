package kopo.poly.globalray.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BookmarkDto {

    private Long bmId;
    private String userId;
    private String articleUrl;
    private LocalDateTime regDt;
}