package com.momently.orchestrator.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * 에이전트 HTTP 클라이언트 공통 설정 바인딩을 검증한다.
 */
class AgentHttpClientPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration.class);

    @Test
    @DisplayName("agents.http 설정이 record에 바인딩된다")
    void bindsPropertiesFromConfiguration() {
        contextRunner
            .withPropertyValues(
                "agents.http.connect-timeout-seconds=9",
                "agents.http.read-timeout-seconds=240",
                "agents.http.max-attempts=4",
                "agents.http.backoff-millis=75"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(AgentHttpClientProperties.class);

                AgentHttpClientProperties properties = context.getBean(AgentHttpClientProperties.class);

                assertThat(properties.connectTimeoutSeconds()).isEqualTo(9);
                assertThat(properties.readTimeoutSeconds()).isEqualTo(240);
                assertThat(properties.maxAttempts()).isEqualTo(4);
                assertThat(properties.backoffMillis()).isEqualTo(75);
            });
    }

    @Configuration(proxyBeanMethods = false)
    @ConfigurationPropertiesScan(basePackageClasses = AgentHttpClientProperties.class)
    static class TestConfiguration {
    }
}
