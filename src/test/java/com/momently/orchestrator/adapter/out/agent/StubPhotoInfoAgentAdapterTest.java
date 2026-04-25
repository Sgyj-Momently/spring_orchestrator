package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 사진 정보 에이전트 대역의 기본 반환 계약을 검증한다.
 */
class StubPhotoInfoAgentAdapterTest {

    @Test
    @DisplayName("프로젝트 식별자를 기준으로 bundle artifact 경로를 반환한다")
    void returnsBundleArtifactPath() {
        StubPhotoInfoAgentAdapter adapter = new StubPhotoInfoAgentAdapter();

        PhotoInfoResult result = adapter.extractPhotoInfo("project-001");

        assertThat(result.photoCount()).isZero();
        assertThat(result.bundlePath()).isEqualTo("artifacts/photo-info/project-001/bundle.json");
    }
}
