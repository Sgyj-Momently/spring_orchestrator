package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 사진 정보 에이전트 대역의 기본 반환 계약을 검증한다.
 */
class StubPhotoInfoAgentAdapterTest {

    @Test
    @DisplayName("프로젝트 식별자를 기준으로 bundle artifact 경로를 반환한다")
    void returnsBundleArtifactPath() throws IOException {
        StubPhotoInfoAgentAdapter adapter = new StubPhotoInfoAgentAdapter();

        PhotoInfoResult result = adapter.extractPhotoInfo("project-001");

        assertThat(result.photoCount()).isZero();
        assertThat(result.bundlePath()).isEqualTo("artifacts/photo-info/project-001/bundle.json");

        Path bundlePath = Path.of(result.bundlePath());
        assertThat(Files.exists(bundlePath)).isTrue();
        String json = Files.readString(bundlePath);
        assertThat(json).contains("\"photo_count\": 0");
        assertThat(json).contains("\"photos\": []");
    }
}
