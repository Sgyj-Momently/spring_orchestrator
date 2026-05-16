package com.momently.orchestrator.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 에이전트 HTTP 클라이언트 공통 설정 record를 검증한다.
 */
class AgentHttpClientPropertiesTest {

    @Test
    @DisplayName("유효한 timeout 값을 그대로 노출한다")
    void exposesConfiguredTimeouts() {
        AgentHttpClientProperties properties = new AgentHttpClientProperties(7, 120, 3, 100);

        assertThat(properties.connectTimeoutSeconds()).isEqualTo(7);
        assertThat(properties.readTimeoutSeconds()).isEqualTo(120);
        assertThat(properties.maxAttempts()).isEqualTo(3);
        assertThat(properties.backoffMillis()).isEqualTo(100);
    }

    @Test
    @DisplayName("0 이하 timeout과 retry 설정은 운영 기본값으로 보정한다")
    void defaultsInvalidTimeouts() {
        AgentHttpClientProperties properties = new AgentHttpClientProperties(0, -1, 0, -1);

        assertThat(properties.connectTimeoutSeconds()).isEqualTo(5);
        assertThat(properties.readTimeoutSeconds()).isEqualTo(300);
        assertThat(properties.maxAttempts()).isEqualTo(2);
        assertThat(properties.backoffMillis()).isEqualTo(250);
    }
}
