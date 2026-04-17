package com.momently.orchestrator.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 사진 그룹화 에이전트 설정 record를 검증한다.
 */
class PhotoGroupingAgentPropertiesTest {

    @Test
    @DisplayName("설정 record는 전달한 값을 그대로 노출한다")
    void exposesConfiguredValues() {
        PhotoGroupingAgentProperties properties = new PhotoGroupingAgentProperties(
            "http://127.0.0.1:8000",
            "/api/v1/photo-groups"
        );

        assertThat(properties.baseUrl()).isEqualTo("http://127.0.0.1:8000");
        assertThat(properties.endpoint()).isEqualTo("/api/v1/photo-groups");
    }
}
