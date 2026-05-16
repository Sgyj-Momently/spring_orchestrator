package com.momently.orchestrator.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.momently.orchestrator.security.UserAccount;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryUserAccountRepositoryTest {

    @Test
    @DisplayName("메모리 저장소에 사용자 계정을 저장하고 조회한다")
    void savesAndFindsAccount() {
        InMemoryUserAccountRepository repository = new InMemoryUserAccountRepository();
        UserAccount account = new UserAccount("member", "hash", Instant.parse("2026-05-17T00:00:00Z"));

        assertThat(repository.existsByUsername("member")).isFalse();

        repository.save(account);

        assertThat(repository.existsByUsername("member")).isTrue();
        assertThat(repository.findByUsername("member")).contains(account);
        assertThat(repository.findByUsername("missing")).isEmpty();
    }
}
