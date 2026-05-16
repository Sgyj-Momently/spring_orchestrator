package com.momently.orchestrator.adapter.out.persistence;

import com.momently.orchestrator.security.UserAccount;
import com.momently.orchestrator.security.UserAccountRepository;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * PostgreSQL 기반 콘솔 사용자 저장소다.
 */
@Repository
@Profile("postgres")
public class PostgresUserAccountRepository implements UserAccountRepository {

    private final JpaUserAccountSpringDataRepository repository;

    public PostgresUserAccountRepository(JpaUserAccountSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return repository.findById(username).map(UserAccountJpaEntity::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsById(username);
    }

    @Override
    public UserAccount save(UserAccount account) {
        return repository.save(UserAccountJpaEntity.fromDomain(account)).toDomain();
    }
}
