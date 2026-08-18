package kopo.poly.globalray.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_INFO")
@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserInfoEntity {

    @Id
    @Column(name = "USER_ID", length = 100)
    private String userId;

    @Column(name = "USER_PW", length = 255, nullable = false)
    private String userPw;

    @Column(name = "USER_NAME", length = 100, nullable = false)
    private String userName;

    @Column(name = "USER_EMAIL", length = 200, nullable = false, unique = true)
    private String userEmail;

    @Column(name = "REG_DT", nullable = false)
    private LocalDateTime regDt;

    @Column(name = "SOCIAL_TYPE", length = 20)
    private String socialType;

    @PrePersist
    public void prePersist() {
        this.regDt = LocalDateTime.now();
    }

    // Setter 대신 의미 있는 메서드로 변경
    public void changePassword(String encodedPw) {
        this.userPw = encodedPw;
    }
}