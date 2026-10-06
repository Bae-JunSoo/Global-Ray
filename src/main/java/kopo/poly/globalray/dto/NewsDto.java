package kopo.poly.globalray.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NewsDto {

    private String articleId;
    private String catType;
    private String title;
    private String titleKor;
    private String sourceName;
    private String author;
    private String url;
    private String description;
    private String summaryKor;
    private String summaryShort;
    private String thumbUrl;
    private LocalDateTime regDt;
    private boolean bookmarked;
    private boolean liked;
    private long viewCount;
    private long likeCount;
    private String country;
}
