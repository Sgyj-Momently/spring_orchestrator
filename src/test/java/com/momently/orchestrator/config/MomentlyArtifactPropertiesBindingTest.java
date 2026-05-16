package com.momently.orchestrator.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * 워크플로 산출물 보존 정책 설정 바인딩을 검증한다.
 */
class MomentlyArtifactPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration.class);

    @Test
    @DisplayName("momently.artifacts 설정이 record에 바인딩된다")
    void bindsPropertiesFromConfiguration() {
        contextRunner
            .withPropertyValues("momently.artifacts.max-edit-versions=8")
            .run(context -> {
                assertThat(context).hasSingleBean(MomentlyArtifactProperties.class);

                MomentlyArtifactProperties properties = context.getBean(MomentlyArtifactProperties.class);

                assertThat(properties.maxEditVersions()).isEqualTo(8);
            });
    }

    @Configuration(proxyBeanMethods = false)
    @ConfigurationPropertiesScan(basePackageClasses = MomentlyArtifactProperties.class)
    static class TestConfiguration {
    }
}
