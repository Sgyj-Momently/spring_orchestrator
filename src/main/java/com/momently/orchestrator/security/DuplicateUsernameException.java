package com.momently.orchestrator.security;

/**
 * 이미 가입된 사용자명일 때 발생한다.
 */
public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String username) {
        super("이미 사용 중인 아이디입니다: " + username);
    }
}
