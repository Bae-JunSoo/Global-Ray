package kopo.poly.globalray.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOGIN_HISTORY")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ")
    private Long seq;

    @Column(name = "USER_ID", length = 100, nullable = false)
    private String userId;

    @Column(name = "USER_NAME", length = 100)
    private String userName;

    @Column(name = "LOGIN_DT", nullable = false)
    private LocalDateTime loginDt;

    @Column(name = "IP_ADDRESS", length = 50)
    private String ipAddress;

    @Column(name = "LOGIN_TYPE", length = 20)
    private String loginType;

    @PrePersist
    public void prePersist() {
        this.loginDt = LocalDateTime.now();
    }
}
