package com.momently.orchestrator.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.momently.orchestrator.config.MomentlySecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 콘솔 계정 로그인 판정을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final MomentlySecurityProperties PROPS = new MomentlySecurityProperties(
        "console",
        "secret-pass",
        "local-dev-console-jwt-signing-secret-min32b",
        7200L
    );

    @Mock
    private JwtService jwtService;

    @Test
    @DisplayName("계정이 일치하면 JWT 문자열을 돌려준다")
    void issuesTokenWhenMatch() {
        AuthService service = new AuthService(PROPS, jwtService);
        when(jwtService.createAccessToken("console")).thenReturn("tok");

        assertThat(service.issueToken("console", "secret-pass")).contains("tok");
        verify(jwtService).createAccessToken("console");
    }

    @Test
    @DisplayName("아이디가 다르면 비어 있다")
    void rejectsWrongUsername() {
        AuthService service = new AuthService(PROPS, jwtService);
        assertThat(service.issueToken("other", "secret-pass")).isEmpty();
    }

    @Test
    @DisplayName("비밀번호가 다르면 비어 있다")
    void rejectsWrongPassword() {
        AuthService service = new AuthService(PROPS, jwtService);
        assertThat(service.issueToken("console", "wrong")).isEmpty();
    }

    @Test
    @DisplayName("설정된 사용자명이 비어 있으면 로그인할 수 없다")
    void rejectsWhenConfiguredUsernameMissing() {
        MomentlySecurityProperties emptyUser = new MomentlySecurityProperties(
            null,
            "secret-pass",
            "local-dev-console-jwt-signing-secret-min32b",
            7200L);
        AuthService service = new AuthService(emptyUser, jwtService);
        assertThat(service.issueToken("console", "secret-pass")).isEmpty();
    }
}
