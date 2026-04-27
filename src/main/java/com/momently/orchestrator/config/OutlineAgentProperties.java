package com.momently.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 개요 생성(outline) FastAPI 에이전트 호출에 필요한 내부 설정이다.
 *
 * @param baseUrl outline 에이전트 base URL
 * @param endpoint outline 요청을 보낼 endpoint 경로
 */
@ConfigurationProperties(prefix = "agents.outline")
public record OutlineAgentProperties(
    String baseUrl,
    String endpoint
) {
}

