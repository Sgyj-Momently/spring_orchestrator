package com.momently.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 민감정보 안전성 검사 FastAPI 에이전트 호출에 필요한 내부 설정이다.
 */
@ConfigurationProperties(prefix = "agents.privacy-safety")
public record PrivacySafetyAgentProperties(
    String baseUrl,
    String endpoint
) {
}

