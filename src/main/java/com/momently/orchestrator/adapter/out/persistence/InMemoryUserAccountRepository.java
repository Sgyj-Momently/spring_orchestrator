package com.momently.orchestrator.adapter.out.persistence;

import com.momently.orchestrator.security.UserAccount;
import com.momently.orchestrator.security.UserAccountRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * memory 프로필에서 쓰는 콘솔 사용자 저장소다.
 */
@Repository
@Profile("memory")
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final Map<String, UserAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(accounts.get(username));
    }

    @Override
    public boolean existsByUsername(String username) {
        return accounts.containsKey(username);
    }

    @Override
    public UserAccount save(UserAccount account) {
        accounts.put(account.username(), account);
        return account;
    }
}
