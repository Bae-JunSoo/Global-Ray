package kopo.poly.globalray.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_LIKE")
@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LIKE_ID")
    private Long likeId;

    @Column(name = "USER_ID", length = 100, nullable = false)
    private String userId;

    @Column(name = "ARTICLE_URL", length = 1000, nullable = false)
    private String articleUrl;

    @Column(name = "REG_DT", nullable = false)
    private LocalDateTime regDt;

    @PrePersist
    public void prePersist() {
        this.regDt = LocalDateTime.now();
    }
}
