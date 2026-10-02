package kopo.poly.globalray.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "CHAT_HISTORY", indexes = @Index(name = "IDX_CHAT_USER_DT", columnList = "USER_ID, REG_DT"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHAT_ID")
    private Long chatId;

    @Column(name = "USER_ID", length = 100, nullable = false)
    private String userId;

    @Column(name = "QUESTION", length = 300, nullable = false)
    private String question;

    @Column(name = "KEYWORDS", length = 200)
    private String keywords;

    @Column(name = "ANSWER", columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(name = "SOURCE_COUNT", nullable = false)
    private int sourceCount;

    @Column(name = "REG_DT", nullable = false)
    private LocalDateTime regDt;

    @PrePersist
    public void prePersist() {
        this.regDt = LocalDateTime.now();
    }
}
