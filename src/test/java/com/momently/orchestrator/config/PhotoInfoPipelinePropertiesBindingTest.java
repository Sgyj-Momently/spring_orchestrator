package com.momently.orchestrator.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * 사진 정보 파이프라인 설정이 스프링 컨텍스트에 바인딩되는지 검증한다.
 */
class PhotoInfoPipelinePropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration.class);

    @Test
    @DisplayName("agents.photo-info.pipeline 설정이 record에 바인딩된다")
    void bindsPropertiesFromConfiguration() {
        contextRunner
            .withPropertyValues(
                "agents.photo-info.pipeline.python-executable=python",
                "agents.photo-info.pipeline.script-path=/workspace/run_pipeline.py",
                "agents.photo-info.pipeline.input-root=/input",
                "agents.photo-info.pipeline.output-root=/output",
                "agents.photo-info.pipeline.ollama-base-url=http://ollama.test",
                "agents.photo-info.pipeline.vision-model=qwen2.5vl:7b",
                "agents.photo-info.pipeline.writer-model=gemma4",
                "agents.photo-info.pipeline.ollama-timeout-seconds=60",
                "agents.photo-info.pipeline.skip-blog=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(PhotoInfoPipelineProperties.class);

                PhotoInfoPipelineProperties properties =
                    context.getBean(PhotoInfoPipelineProperties.class);

                assertThat(properties.pythonExecutable()).isEqualTo("python");
                assertThat(properties.scriptPath()).isEqualTo("/workspace/run_pipeline.py");
                assertThat(properties.inputRoot()).isEqualTo("/input");
                assertThat(properties.outputRoot()).isEqualTo("/output");
                assertThat(properties.ollamaBaseUrl()).isEqualTo("http://ollama.test");
                assertThat(properties.visionModel()).isEqualTo("qwen2.5vl:7b");
                assertThat(properties.writerModel()).isEqualTo("gemma4");
                assertThat(properties.ollamaTimeoutSeconds()).isEqualTo(60);
                assertThat(properties.skipBlog()).isFalse();
            });
    }

    @Configuration(proxyBeanMethods = false)
    @ConfigurationPropertiesScan(basePackageClasses = PhotoInfoPipelineProperties.class)
    static class TestConfiguration {
    }
}
