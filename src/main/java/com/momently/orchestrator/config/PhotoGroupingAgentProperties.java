package com.momently.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 사진 그룹화 FastAPI 에이전트 호출에 필요한 내부 설정이다.
 *
 * <p>이 값들은 공개 워크플로 생성 요청에서 받지 않는다. 배포 환경별 그룹화 에이전트 주소와
 * API 경로를 Spring 설정으로만 관리해, 사용자가 인프라 세부사항을 조작하지 못하게 한다.</p>
 *
 * @param baseUrl 그룹화 에이전트의 scheme, host, port를 포함한 base URL
 * @param endpoint 그룹화 요청을 보낼 FastAPI endpoint 경로
 */
@ConfigurationProperties(prefix = "agents.photo-grouping")
public record PhotoGroupingAgentProperties(
    String baseUrl,
    String endpoint
) {
}
