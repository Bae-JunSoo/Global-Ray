package kopo.poly.globalray.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "VIEW_HISTORY")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewHistoryEntity {

    @Id
    private String id;

    @Field("USER_ID")
    private String userId;

    @Field("ARTICLE_ID")
    private String articleId;

    @Field("TITLE")
    private String title;

    @Field("VIEW_DT")
    private LocalDateTime viewDt;
}
