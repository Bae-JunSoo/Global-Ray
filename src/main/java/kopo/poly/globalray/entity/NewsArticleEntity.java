package kopo.poly.globalray.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;

@Document(collection = "NEWS_LIST")
@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NewsArticleEntity {

    @Id
    private String articleId;

    @Field("CAT_TYPE")
    private String catType;

    @Field("TITLE")
    private String title;

    @Field("TITLE_KOR")  // 한국어 번역 제목 추가
    private String titleKor;

    @Field("SOURCE_NAME")
    private String sourceName;

    @Field("AUTHOR")
    private String author;

    @Field("URL")
    @Indexed(unique = true)
    private String url;

    @Field("DESCRIPTION")
    private String description;

    @Field("SUMMARY_KOR")
    private String summaryKor;

    @Field("SUMMARY_SHORT")
    private String summaryShort;

    @Field("THUMB_URL")
    private String thumbUrl;

    @Field("CONTENT_FULL")
    private String contentFull;

    @Field("REG_DT")
    private LocalDateTime regDt;

    @Builder.Default
    @Field("VIEW_COUNT")
    private long viewCount = 0L;

    @Builder.Default
    @Field("LIKE_COUNT")
    private long likeCount = 0L;

    public void updateSummaryKor(String summaryKor) {
        this.summaryKor = summaryKor;
    }
}