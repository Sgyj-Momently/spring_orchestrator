package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 사진 그룹화 에이전트 대역의 기본 반환 계약을 검증한다.
 */
class StubPhotoGroupingAgentAdapterTest {

    @Test
    @DisplayName("워크플로의 그룹화 전략을 결과에 반영한다")
    void returnsGroupingStrategyFromWorkflow() throws IOException {
        StubPhotoGroupingAgentAdapter adapter = new StubPhotoGroupingAgentAdapter();

        PhotoGroupingResult result = adapter.groupPhotos(
            "project-001",
            "TIME_BASED",
            90,
            new PhotoInfoResult(0, "artifacts/photo-info/project-001/bundle.json")
        );

        assertThat(result.groupingStrategy()).isEqualTo("TIME_BASED");
        assertThat(result.groupCount()).isZero();
        assertThat(result.resultPath()).isEqualTo("artifacts/photo-grouping/project-001/grouping-result.json");

        Path artifactPath = Path.of(result.resultPath());
        assertThat(Files.exists(artifactPath)).isTrue();
        String json = Files.readString(artifactPath);
        assertThat(json).contains("\"grouping_strategy\": \"TIME_BASED\"");
        assertThat(json).contains("\"time_window_minutes\": 90");
        assertThat(json).contains("\"groups\": []");
    }
}
