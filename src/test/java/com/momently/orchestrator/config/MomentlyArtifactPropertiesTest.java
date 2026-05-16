package com.momently.orchestrator.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 워크플로 산출물 보존 정책 설정을 검증한다.
 */
class MomentlyArtifactPropertiesTest {

    @Test
    @DisplayName("수정본 보존 개수를 그대로 노출한다")
    void exposesConfiguredMaxEditVersions() {
        MomentlyArtifactProperties properties = new MomentlyArtifactProperties(7);

        assertThat(properties.maxEditVersions()).isEqualTo(7);
    }

    @Test
    @DisplayName("음수 보존 개수는 기본값으로 보정한다")
    void defaultsNegativeMaxEditVersions() {
        MomentlyArtifactProperties properties = new MomentlyArtifactProperties(-1);

        assertThat(properties.maxEditVersions()).isEqualTo(20);
    }
}
