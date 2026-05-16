package com.momently.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FastAPI 에이전트 HTTP 호출에 공통으로 적용할 클라이언트 제한값이다.
 */
@ConfigurationProperties(prefix = "agents.http")
public record AgentHttpClientProperties(
    int connectTimeoutSeconds,
    int readTimeoutSeconds,
    int maxAttempts,
    int backoffMillis
) {

    public AgentHttpClientProperties {
        if (connectTimeoutSeconds <= 0) {
            connectTimeoutSeconds = 5;
        }
        if (readTimeoutSeconds <= 0) {
            readTimeoutSeconds = 300;
        }
        if (maxAttempts <= 0) {
            maxAttempts = 2;
        }
        if (backoffMillis < 0) {
            backoffMillis = 250;
        }
    }
}
