package com.momently.orchestrator.config;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * 콘솔 로그인 계정 및 JWT 설정이다.
 *
 * <p>운영에서는 비밀번호·시크릿을 환경 변수로만 주입하고 저장소 로그에 남기지 마라.</p>
 *
 * @param username 콘솔 로그인 아이디
 * @param password 콘솔 로그인 비밀번호(평문. 운영은 비밀 관리 도구만 사용할 것)
 * @param jwtSecret HS256 비밀 키(UTF-8 기준 최소 32바이트 권장)
 * @param jwtExpirationSeconds 발급 토큰 유효 기간
 * @param signupInviteCode 회원가입 초대 코드. 비어 있으면 회원가입 비활성화
 */
@ConfigurationProperties(prefix = "momently.security")
public record MomentlySecurityProperties(
    String username,
    String password,
    String jwtSecret,
    long jwtExpirationSeconds,
    String signupInviteCode
) {

    // 생성자가 2개라 @ConfigurationProperties 바인딩 대상을 명시해야 한다.
    // record 컴팩트 생성자에 @ConstructorBinding 을 달면 다중 생성자 상황에서
    // Spring 이 바인딩 생성자를 못 찾고 기본 생성자로 폴백해 기동이 실패한다.
    // 정규 생성자를 명시적으로 작성하고 거기에 @ConstructorBinding 을 둔다.
    @ConstructorBinding
    public MomentlySecurityProperties(
        String username,
        String password,
        String jwtSecret,
        long jwtExpirationSeconds,
        String signupInviteCode
    ) {
        this.username = username;
        this.password = password;
        this.jwtSecret = jwtSecret;
        this.jwtExpirationSeconds = jwtExpirationSeconds <= 0 ? 86_400L : jwtExpirationSeconds;
        this.signupInviteCode = signupInviteCode;
    }

    public MomentlySecurityProperties(
        String username,
        String password,
        String jwtSecret,
        long jwtExpirationSeconds
    ) {
        this(username, password, jwtSecret, jwtExpirationSeconds, "");
    }

    public void validateJwtSecretStrength() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("momently.security.jwt-secret 을 설정하세요.");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT 시크릿은 UTF-8 기준 최소 32바이트 이상으로 설정하세요.");
        }
    }

    public boolean signupEnabled() {
        return signupInviteCode != null && !signupInviteCode.isBlank();
    }
}
