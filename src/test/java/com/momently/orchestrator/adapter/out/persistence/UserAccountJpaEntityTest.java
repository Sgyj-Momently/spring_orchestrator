package com.momently.orchestrator.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.momently.orchestrator.security.UserAccount;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserAccountJpaEntityTest {

    @Test
    @DisplayName("도메인 계정과 JPA 엔티티를 왕복 변환한다")
    void mapsDomainRoundTrip() {
        UserAccount account = new UserAccount("member", "hash", Instant.parse("2026-05-17T00:00:00Z"));

        UserAccount mapped = UserAccountJpaEntity.fromDomain(account).toDomain();

        assertThat(mapped).isEqualTo(account);
    }
}
