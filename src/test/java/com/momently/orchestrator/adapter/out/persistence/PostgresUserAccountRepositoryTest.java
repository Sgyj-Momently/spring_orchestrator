package com.momently.orchestrator.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.momently.orchestrator.security.UserAccount;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresUserAccountRepositoryTest {

    @Mock
    private JpaUserAccountSpringDataRepository springDataRepository;

    @Test
    @DisplayName("Spring Data 저장소를 통해 사용자 계정을 조회·저장한다")
    void delegatesToSpringDataRepository() {
        PostgresUserAccountRepository repository = new PostgresUserAccountRepository(springDataRepository);
        UserAccount account = new UserAccount("member", "hash", Instant.parse("2026-05-17T00:00:00Z"));
        UserAccountJpaEntity entity = UserAccountJpaEntity.fromDomain(account);
        when(springDataRepository.findById("member")).thenReturn(Optional.of(entity));
        when(springDataRepository.existsById("member")).thenReturn(true);
        when(springDataRepository.save(org.mockito.ArgumentMatchers.any(UserAccountJpaEntity.class))).thenReturn(entity);

        assertThat(repository.findByUsername("member")).contains(account);
        assertThat(repository.existsByUsername("member")).isTrue();
        assertThat(repository.save(account)).isEqualTo(account);

        verify(springDataRepository).save(org.mockito.ArgumentMatchers.any(UserAccountJpaEntity.class));
    }
}
