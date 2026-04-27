package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StubOutlineAgentAdapterTest {

    @Test
    @DisplayName("stub 개요 에이전트는 그룹 수를 섹션 수로 반환한다")
    void returnsDeterministicOutlineResult() {
        StubOutlineAgentAdapter adapter = new StubOutlineAgentAdapter();

        OutlineResult result = adapter.createOutline(
            "project-001",
            new PhotoInfoResult(2, "output/project-001/bundles/bundle.json"),
            new PhotoGroupingResult("LOCATION_BASED", 4, "output/project-001/grouping/grouping-result.json"),
            new HeroPhotoResult(4, "output/project-001/hero-photo/hero-result.json")
        );

        assertThat(result.outlineSectionCount()).isEqualTo(4);
        assertThat(result.resultPath()).isEqualTo("output/project-001/outline/outline.json");
    }
}
