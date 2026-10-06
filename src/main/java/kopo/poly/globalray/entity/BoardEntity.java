package kopo.poly.globalray.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "board")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class BoardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 50)
    private String userId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime regDt;

    // 조회수 증가 때는 바뀌면 안 되므로 @UpdateTimestamp 대신 update()에서만 직접 기록
    private LocalDateTime modDt;

    @Column(nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BoardCommentEntity> comments = new ArrayList<>();

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        this.modDt = LocalDateTime.now();
    }
}
