package com.momently.orchestrator.security;

import com.momently.orchestrator.config.MomentlySecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 설정에 정의된 콘솔 계정으로만 로그인을 허용한다.
 *
 * <p>다중 사용자·DB 연동 전 단계이다.</p>
 */
@Service
public class AuthService {

    private final MomentlySecurityProperties properties;
    private final JwtService jwtService;

    public AuthService(MomentlySecurityProperties properties, JwtService jwtService) {
        this.properties = properties;
        this.jwtService = jwtService;
    }

    public Optional<String> issueToken(String username, String password) {
        if (!constantTimeEquals(properties.username(), username)) {
            return Optional.empty();
        }
        if (!constantTimeEquals(properties.password(), password)) {
            return Optional.empty();
        }
        return Optional.of(jwtService.createAccessToken(username));
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
