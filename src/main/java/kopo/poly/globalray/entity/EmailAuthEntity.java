package kopo.poly.globalray.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "EMAIL_AUTH")
@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EmailAuthEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUTH_ID")
    private Long authId;

    @Column(name = "REQ_EMAIL", length = 200, nullable = false)
    private String reqEmail;

    // SHA256 암호화된 인증코드 저장 (64자)
    @Column(name = "AUTH_CODE", length = 64, nullable = false)
    private String authCode;

    @Column(name = "IS_VERIFIED", nullable = false)
    private Integer isVerified;

    @Column(name = "EXPIRE_DT", nullable = false)
    private LocalDateTime expireDt;

    // 기존 행이 있는 테이블에 컬럼이 추가되므로 DB 기본값 0을 함께 지정
    @Builder.Default
    @Column(name = "FAIL_COUNT", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    private int failCount = 0;

    // Setter 대신 의미 있는 메서드로 변경
    public void markVerified() {
        this.isVerified = 1;
    }

    public void increaseFailCount() {
        this.failCount++;
    }
}