package kopo.poly.globalray.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_SESSION")
@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserSessionEntity {

    @Id
    @Column(name = "SESSION_ID", length = 255)
    private String sessionId;

    @Column(name = "USER_ID", length = 100)
    private String userId;

    @Column(name = "LAST_ACCESS", nullable = false)
    private LocalDateTime lastAccess;

    @Column(name = "EXPIRE_DT", nullable = false)
    private LocalDateTime expireDt;
}