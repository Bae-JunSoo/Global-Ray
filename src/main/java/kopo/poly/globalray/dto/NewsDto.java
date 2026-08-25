package kopo.poly.globalray.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NewsDto {

    private String articleId;
    private String catType;
    private String title;
    private String titleKor;     // 한국어 번역 제목 추가
    private String sourceName;
    private String author;
    private String url;
    private String description;
    private String summaryKor;
    private String summaryShort;
    private String thumbUrl;
    private LocalDateTime regDt;
    private boolean bookmarked;
    private long viewCount;

    // 페이징 정보
    private int currentPage;
    private int totalPages;
    private long totalElements;
}