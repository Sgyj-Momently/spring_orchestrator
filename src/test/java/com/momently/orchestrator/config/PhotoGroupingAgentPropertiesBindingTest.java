package com.momently.orchestrator.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * 사진 그룹화 에이전트 설정이 스프링 컨텍스트에 바인딩되는지 검증한다.
 */
class PhotoGroupingAgentPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration.class);

    @Test
    @DisplayName("agents.photo-grouping 설정이 record에 바인딩된다")
    void bindsPropertiesFromConfiguration() {
        contextRunner
            .withPropertyValues(
                "agents.photo-grouping.base-url=http://photo-grouping.test",
                "agents.photo-grouping.endpoint=/api/v1/photo-groups"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(PhotoGroupingAgentProperties.class);

                PhotoGroupingAgentProperties properties =
                    context.getBean(PhotoGroupingAgentProperties.class);

                assertThat(properties.baseUrl()).isEqualTo("http://photo-grouping.test");
                assertThat(properties.endpoint()).isEqualTo("/api/v1/photo-groups");
            });
    }

    @Configuration(proxyBeanMethods = false)
    @ConfigurationPropertiesScan(basePackageClasses = PhotoGroupingAgentProperties.class)
    static class TestConfiguration {
    }
}
