package com.momently.orchestrator.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 사진 정보 파이프라인 설정 record를 검증한다.
 */
class PhotoInfoPipelinePropertiesTest {

    @Test
    @DisplayName("비어 있는 설정값에는 로컬 개발 기본값을 채운다")
    void fillsDefaultValues() {
        PhotoInfoPipelineProperties properties = new PhotoInfoPipelineProperties(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            true,
            false
        );

        assertThat(properties.pythonExecutable()).isEqualTo("python3");
        assertThat(properties.scriptPath()).isEqualTo("../photo_exif_llm_pipeline/src/run_pipeline.py");
        assertThat(properties.inputRoot()).isEqualTo("../photo_exif_llm_pipeline/input_photos");
        assertThat(properties.outputRoot()).isEqualTo("../photo_exif_llm_pipeline/output/orchestrator");
        assertThat(properties.ollamaBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(properties.visionModel()).isEqualTo("qwen2.5vl:7b");
        assertThat(properties.writerModel()).isEqualTo("qwen2.5:14b");
        assertThat(properties.ollamaTimeoutSeconds()).isEqualTo(180);
        assertThat(properties.ffmpegCommand()).isEqualTo("ffmpeg");
        assertThat(properties.videoFrameSecond()).isEqualTo(1.0);
        assertThat(properties.videoFrameCount()).isEqualTo(3);
        assertThat(properties.videoFrameIntervalSeconds()).isEqualTo(4.0);
        assertThat(properties.skipBlog()).isTrue();
        assertThat(properties.force()).isFalse();
    }

    @Test
    @DisplayName("명시한 설정값은 그대로 노출한다")
    void exposesConfiguredValues() {
        PhotoInfoPipelineProperties properties = new PhotoInfoPipelineProperties(
            "python",
            "/workspace/run_pipeline.py",
            "/input",
            "/output",
            "http://ollama.test",
            "qwen2.5vl:7b",
            "gemma4",
            60,
            "/usr/bin/ffmpeg",
            2.5,
            5,
            6.5,
            false,
            true
        );

        assertThat(properties.pythonExecutable()).isEqualTo("python");
        assertThat(properties.scriptPath()).isEqualTo("/workspace/run_pipeline.py");
        assertThat(properties.inputRoot()).isEqualTo("/input");
        assertThat(properties.outputRoot()).isEqualTo("/output");
        assertThat(properties.ollamaBaseUrl()).isEqualTo("http://ollama.test");
        assertThat(properties.visionModel()).isEqualTo("qwen2.5vl:7b");
        assertThat(properties.writerModel()).isEqualTo("gemma4");
        assertThat(properties.ollamaTimeoutSeconds()).isEqualTo(60);
        assertThat(properties.ffmpegCommand()).isEqualTo("/usr/bin/ffmpeg");
        assertThat(properties.videoFrameSecond()).isEqualTo(2.5);
        assertThat(properties.videoFrameCount()).isEqualTo(5);
        assertThat(properties.videoFrameIntervalSeconds()).isEqualTo(6.5);
        assertThat(properties.skipBlog()).isFalse();
        assertThat(properties.force()).isTrue();
    }
}
