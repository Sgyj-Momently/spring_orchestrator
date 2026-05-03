package com.momently.orchestrator.security;

import com.momently.orchestrator.config.MomentlySecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * 콘솔용 접근 토큰(JWT) 생성·검증이다.
 */
@Service
public class JwtService {

    private final MomentlySecurityProperties properties;
    private final SecretKey secretKey;

    public JwtService(MomentlySecurityProperties properties) {
        this.properties = properties;
        properties.validateJwtSecretStrength();
        this.secretKey = resolveKey(properties.jwtSecret());
    }

    private static SecretKey resolveKey(String jwtSecret) {
        String trimmed = jwtSecret.strip();
        if (trimmed.startsWith("base64:")) {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(trimmed.substring("base64:".length())));
        }
        return Keys.hmacShaKeyFor(trimmed.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String subject) {
        long nowMs = System.currentTimeMillis();
        long expMs = nowMs + properties.jwtExpirationSeconds() * 1000L;
        return Jwts.builder()
            .subject(subject)
            .issuedAt(new Date(nowMs))
            .expiration(new Date(expMs))
            .signWith(secretKey)
            .compact();
    }

    /** 유효한 토큰이면 클레임을 돌려주고 아니면 비어 있는 값이다. */
    public Optional<Claims> parseValidClaims(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build()
                .parseSignedClaims(token).getPayload();
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
