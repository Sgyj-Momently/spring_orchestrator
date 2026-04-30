package com.momently.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 문체 적용(style) FastAPI 에이전트 호출에 필요한 내부 설정이다.
 */
@ConfigurationProperties(prefix = "agents.style")
public record StyleAgentProperties(
    String baseUrl,
    String endpoint
) {
}

