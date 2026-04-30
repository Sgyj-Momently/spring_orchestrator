package com.momently.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 최종 검수(review) FastAPI 에이전트 호출에 필요한 내부 설정이다.
 */
@ConfigurationProperties(prefix = "agents.review")
public record ReviewAgentProperties(
    String baseUrl,
    String endpoint
) {
}

