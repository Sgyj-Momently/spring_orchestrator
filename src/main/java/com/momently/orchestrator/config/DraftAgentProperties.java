package com.momently.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 초안 작성(draft) FastAPI 에이전트 호출에 필요한 내부 설정이다.
 */
@ConfigurationProperties(prefix = "agents.draft")
public record DraftAgentProperties(
    String baseUrl,
    String endpoint
) {
}

