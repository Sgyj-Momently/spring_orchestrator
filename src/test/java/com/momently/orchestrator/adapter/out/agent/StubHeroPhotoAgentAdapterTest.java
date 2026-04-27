package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StubHeroPhotoAgentAdapterTest {

    @Test
    @DisplayName("stub 대표 사진 에이전트는 그룹 수를 대표 사진 수로 반환한다")
    void returnsDeterministicHeroPhotoResult() {
        StubHeroPhotoAgentAdapter adapter = new StubHeroPhotoAgentAdapter();

        HeroPhotoResult result = adapter.selectHeroPhotos(
            "project-001",
            new PhotoInfoResult(2, "output/project-001/bundles/bundle.json"),
            new PhotoGroupingResult("LOCATION_BASED", 3, "output/project-001/grouping/grouping-result.json")
        );

        assertThat(result.heroPhotoCount()).isEqualTo(3);
        assertThat(result.resultPath()).isEqualTo("output/project-001/hero-photo/hero-result.json");
    }
}
