package com.momently.orchestrator.security;

/**
 * 회원가입이 설정으로 비활성화되어 있을 때 발생한다.
 */
public class SignupDisabledException extends RuntimeException {

    public SignupDisabledException() {
        super("회원가입이 비활성화되어 있습니다.");
    }
}
