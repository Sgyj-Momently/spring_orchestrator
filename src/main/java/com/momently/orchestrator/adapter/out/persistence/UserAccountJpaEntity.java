package com.momently.orchestrator.adapter.out.persistence;

import com.momently.orchestrator.security.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 콘솔 사용자 계정 JPA 엔티티다.
 */
@Entity
@Table(name = "momently_user_accounts")
class UserAccountJpaEntity {

    @Id
    @Column(name = "username", nullable = false, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserAccountJpaEntity() {
    }

    static UserAccountJpaEntity fromDomain(UserAccount account) {
        UserAccountJpaEntity entity = new UserAccountJpaEntity();
        entity.username = account.username();
        entity.passwordHash = account.passwordHash();
        entity.createdAt = account.createdAt();
        return entity;
    }

    UserAccount toDomain() {
        return new UserAccount(username, passwordHash, createdAt);
    }
}
