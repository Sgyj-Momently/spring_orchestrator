package com.momently.orchestrator.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momently.orchestrator.config.MomentlySecurityProperties;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JWT 생성·검증 동작을 검증한다.
 */
class JwtServiceTest {

    private static MomentlySecurityProperties props(String secret, long expSec) {
        return new MomentlySecurityProperties("u", "p", secret, expSec);
    }

    @Test
    @DisplayName("시크릿이 짧으면 기동 시 예외다")
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService(props("short", 3600)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("시크릿이 비어 있으면 기동 시 예외다")
    void rejectsBlankSecret() {
        assertThatThrownBy(() -> new JwtService(new MomentlySecurityProperties("u", "p", null, 3600)))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(new MomentlySecurityProperties("u", "p", "   ", 3600)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("발급한 토큰은 동일 키로 검증된다")
    void roundTripToken() {
        JwtService jwt = new JwtService(props("local-dev-console-jwt-signing-secret-min32b", 3600));
        String token = jwt.createAccessToken("console");
        assertThat(jwt.parseValidClaims(token)).isPresent();
        assertThat(jwt.parseValidClaims(token).get().getSubject()).isEqualTo("console");
    }

    @Test
    @DisplayName("빈 토큰·틀린 토큰은 비어 있다")
    void rejectsInvalidToken() {
        JwtService jwt = new JwtService(props("local-dev-console-jwt-signing-secret-min32b", 3600));
        assertThat(jwt.parseValidClaims("")).isEmpty();
        assertThat(jwt.parseValidClaims("not.a.jwt")).isEmpty();
    }

    @Test
    @DisplayName("다른 키로는 검증되지 않는다")
    void rejectsForeignKey() {
        JwtService a = new JwtService(props("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 3600));
        JwtService b = new JwtService(props("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", 3600));
        String token = a.createAccessToken("sub");
        assertThat(b.parseValidClaims(token)).isEqualTo(Optional.empty());
    }

    @Test
    @DisplayName("base64: 접두가 있으면 디코드한 바이트로 서명한다")
    void parsesBase64SecretPrefix() {
        byte[] raw = new byte[32];
        raw[7] = 3;
        String b64 = Base64.getEncoder().encodeToString(raw);
        JwtService jwt = new JwtService(props("base64:" + b64, 300));
        String token = jwt.createAccessToken("u");
        assertThat(jwt.parseValidClaims(token).get().getSubject()).isEqualTo("u");
    }
}
