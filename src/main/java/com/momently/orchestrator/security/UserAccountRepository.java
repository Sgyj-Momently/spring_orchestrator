package com.momently.orchestrator.security;

import java.util.Optional;

/**
 * 콘솔 사용자 계정 저장소 포트다.
 */
public interface UserAccountRepository {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    UserAccount save(UserAccount account);
}
