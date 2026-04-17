package com.momently.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 사진 그룹화 에이전트 호출 설정을 표현한다.
 *
 * @param baseUrl 그룹화 에이전트 base URL
 * @param endpoint 그룹화 요청 엔드포인트 경로
 */
@ConfigurationProperties(prefix = "agents.photo-grouping")
public record PhotoGroupingAgentProperties(
    String baseUrl,
    String endpoint
) {
}
