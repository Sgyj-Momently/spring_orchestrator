package com.momently.orchestrator.config;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 콘솔 로그인 계정 및 JWT 설정이다.
 *
 * <p>운영에서는 비밀번호·시크릿을 환경 변수로만 주입하고 저장소 로그에 남기지 마라.</p>
 *
 * @param username 콘솔 로그인 아이디
 * @param password 콘솔 로그인 비밀번호(평문. 운영은 비밀 관리 도구만 사용할 것)
 * @param jwtSecret HS256 비밀 키(UTF-8 기준 최소 32바이트 권장)
 * @param jwtExpirationSeconds 발급 토큰 유효 기간
 */
@ConfigurationProperties(prefix = "momently.security")
public record MomentlySecurityProperties(
    String username,
    String password,
    String jwtSecret,
    long jwtExpirationSeconds
) {

    public MomentlySecurityProperties {
        if (jwtExpirationSeconds <= 0) {
            jwtExpirationSeconds = 86_400L;
        }
    }

    public void validateJwtSecretStrength() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("momently.security.jwt-secret 을 설정하세요.");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT 시크릿은 UTF-8 기준 최소 32바이트 이상으로 설정하세요.");
        }
    }
}
