package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 사진 그룹화 에이전트 대역의 기본 반환 계약을 검증한다.
 */
class StubPhotoGroupingAgentAdapterTest {

    @Test
    @DisplayName("payload의 그룹화 전략을 결과에 반영한다")
    void returnsGroupingStrategyFromPayload() {
        StubPhotoGroupingAgentAdapter adapter = new StubPhotoGroupingAgentAdapter();

        PhotoGroupingResult result = adapter.groupPhotos(Map.of("grouping_strategy", "TIME_BASED"));

        assertThat(result.groupingStrategy()).isEqualTo("TIME_BASED");
        assertThat(result.groupCount()).isZero();
    }
}
