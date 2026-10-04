package kr.antimony.accounts.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

// 관리자 한 명. 회원 번호(users.id)가 기본 키다. 관리자 지정·해제는 이 테이블의 행을 넣고 지우는 것으로 한다.
@Table(name = "admins")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Admin {
    @Id
    @Column(name = "user_id", updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, length = 20)
    private AdminRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Admin(Long userId, AdminRole role, Instant createdAt) {
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
    }
}
