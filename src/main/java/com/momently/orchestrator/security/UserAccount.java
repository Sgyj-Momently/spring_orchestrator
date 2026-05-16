package com.momently.orchestrator.security;

import java.time.Instant;

/**
 * 콘솔에 로그인할 수 있는 사용자 계정이다.
 *
 * @param username 로그인 아이디
 * @param passwordHash 해시된 비밀번호
 * @param createdAt 생성 시각
 */
public record UserAccount(String username, String passwordHash, Instant createdAt) {
}
