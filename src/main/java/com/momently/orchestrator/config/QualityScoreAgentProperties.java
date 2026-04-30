package com.momently.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 사진 품질 점수화 FastAPI 에이전트 호출에 필요한 내부 설정이다.
 */
@ConfigurationProperties(prefix = "agents.quality-score")
public record QualityScoreAgentProperties(
    String baseUrl,
    String endpoint
) {
}

