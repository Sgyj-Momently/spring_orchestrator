package com.momently.orchestrator.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 브라우저 CORS 허용 출처 설정이다.
 *
 * @param cors API에 대한 교차 출처 요청 허용 값
 */
@ConfigurationProperties(prefix = "momently.web")
public record MomentlyWebProperties(Cors cors) {

    /** {@link #allowedOrigins()} 가 비었을 때 안전하게 기본 목록만 쓴다. */
    public List<String> corsAllowedOriginPatterns() {
        if (cors == null || cors.allowedOrigins() == null || cors.allowedOrigins().isEmpty()) {
            return List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
            );
        }
        List<String> cleaned = cors.allowedOrigins().stream()
            .filter(o -> o != null && !o.isBlank())
            .map(String::strip)
            .toList();
        return cleaned.isEmpty()
            ? List.of("http://localhost:5173", "http://127.0.0.1:5173")
            : cleaned;
    }

    /**
     * @param allowedPatterns 호스트·스킴 허용({@link org.springframework.web.cors.CorsConfiguration} 패턴)
     */
    public record Cors(List<String> allowedOrigins) {
    }
}
