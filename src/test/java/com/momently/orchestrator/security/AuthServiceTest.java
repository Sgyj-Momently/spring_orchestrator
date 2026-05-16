package com.momently.orchestrator.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.momently.orchestrator.config.MomentlySecurityProperties;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("계정이 일치하면 JWT 문자열을 돌려준다")
    void issuesTokenWhenMatch() {
        AuthService service = service(PROPS);
        when(jwtService.createAccessToken("console")).thenReturn("tok");

        assertThat(service.issueToken("console", "secret-pass")).contains("tok");
        verify(jwtService).createAccessToken("console");
    }

    @Test
    @DisplayName("가입된 DB 사용자가 있으면 해시 비밀번호로 로그인한다")
    void issuesTokenForRegisteredUser() {
        AuthService service = service(PROPS);
        when(userAccountRepository.findByUsername("member"))
            .thenReturn(Optional.of(new UserAccount("member", "hash", Instant.parse("2026-05-17T00:00:00Z"))));
        when(passwordEncoder.matches("secret-pass", "hash")).thenReturn(true);
        when(jwtService.createAccessToken("member")).thenReturn("member-token");

        assertThat(service.issueToken("member", "secret-pass")).contains("member-token");
    }

    @Test
    @DisplayName("가입된 DB 사용자의 비밀번호가 다르면 설정 계정 fallback을 타지 않는다")
    void rejectsWrongPasswordForRegisteredUser() {
        AuthService service = service(PROPS);
        when(userAccountRepository.findByUsername("console"))
            .thenReturn(Optional.of(new UserAccount("console", "hash", Instant.parse("2026-05-17T00:00:00Z"))));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThat(service.issueToken("console", "wrong")).isEmpty();
    }

    @Test
    @DisplayName("아이디가 다르면 비어 있다")
    void rejectsWrongUsername() {
        AuthService service = service(PROPS);
        assertThat(service.issueToken("other", "secret-pass")).isEmpty();
    }

    @Test
    @DisplayName("비밀번호가 다르면 비어 있다")
    void rejectsWrongPassword() {
        AuthService service = service(PROPS);
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
        AuthService service = service(emptyUser);
        assertThat(service.issueToken("console", "secret-pass")).isEmpty();
    }

    @Test
    @DisplayName("초대 코드가 맞으면 사용자 계정을 만들고 토큰을 발급한다")
    void registersUserWithInviteCode() {
        MomentlySecurityProperties props = new MomentlySecurityProperties(
            "console",
            "secret-pass",
            "local-dev-console-jwt-signing-secret-min32b",
            7200L,
            "invite-123"
        );
        AuthService service = service(props);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded");
        when(jwtService.createAccessToken("new-user")).thenReturn("new-token");

        String token = service.registerAndIssueToken("new-user", "new-password", "invite-123");

        assertThat(token).isEqualTo("new-token");
        verify(userAccountRepository).save(org.mockito.ArgumentMatchers.argThat(account ->
            account.username().equals("new-user") && account.passwordHash().equals("encoded")
        ));
    }

    @Test
    @DisplayName("초대 코드가 비어 있으면 회원가입은 비활성화된다")
    void rejectsSignupWhenInviteMissing() {
        AuthService service = service(PROPS);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.registerAndIssueToken("new-user", "new-password", "anything"))
            .isInstanceOf(SignupDisabledException.class);
    }

    @Test
    @DisplayName("중복 아이디 가입은 거절한다")
    void rejectsDuplicateSignup() {
        MomentlySecurityProperties props = new MomentlySecurityProperties(
            "console",
            "secret-pass",
            "local-dev-console-jwt-signing-secret-min32b",
            7200L,
            "invite-123"
        );
        AuthService service = service(props);
        when(userAccountRepository.existsByUsername("new-user")).thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.registerAndIssueToken("new-user", "new-password", "invite-123"))
            .isInstanceOf(DuplicateUsernameException.class);
    }

    @Test
    @DisplayName("설정 콘솔 계정과 같은 아이디는 가입할 수 없다")
    void rejectsSignupWithFallbackConsoleUsername() {
        MomentlySecurityProperties props = new MomentlySecurityProperties(
            "console",
            "secret-pass",
            "local-dev-console-jwt-signing-secret-min32b",
            7200L,
            "invite-123"
        );
        AuthService service = service(props);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.registerAndIssueToken("console", "new-password", "invite-123"))
            .isInstanceOf(DuplicateUsernameException.class);
    }

    @Test
    @DisplayName("회원가입 입력값을 검증한다")
    void validatesSignupFields() {
        MomentlySecurityProperties props = new MomentlySecurityProperties(
            "console",
            "secret-pass",
            "local-dev-console-jwt-signing-secret-min32b",
            7200L,
            "invite-123"
        );
        AuthService service = service(props);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.registerAndIssueToken("bad user", "new-password", "invite-123"))
            .isInstanceOf(IllegalArgumentException.class);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.registerAndIssueToken("new-user", "short", "invite-123"))
            .isInstanceOf(IllegalArgumentException.class);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.registerAndIssueToken("new-user", "new-password", "wrong"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private AuthService service(MomentlySecurityProperties props) {
        return new AuthService(props, jwtService, userAccountRepository, passwordEncoder);
    }
}
