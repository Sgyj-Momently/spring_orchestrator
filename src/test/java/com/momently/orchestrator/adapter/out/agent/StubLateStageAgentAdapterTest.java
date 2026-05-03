package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.momently.orchestrator.application.port.out.result.DraftResult;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.ReviewResult;
import com.momently.orchestrator.application.port.out.result.StyleResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StubLateStageAgentAdapterTest {

    @Test
    @DisplayName("late-stage stub adapters return deterministic artifact references")
    void returnsDeterministicArtifacts() throws IOException {
        StubDraftAgentAdapter draftAdapter = new StubDraftAgentAdapter();
        StubStyleAgentAdapter styleAdapter = new StubStyleAgentAdapter();
        StubReviewAgentAdapter reviewAdapter = new StubReviewAgentAdapter();

        DraftResult draft = draftAdapter.createDraft(
            "project-001",
            new PhotoInfoResult(2, "bundle.json"),
            new PhotoGroupingResult("TIME_BASED", 1, "grouping.json"),
            new HeroPhotoResult(1, "hero.json"),
            new OutlineResult(2, "outline.json")
        );
        StyleResult style = styleAdapter.applyStyle("project-001", draft, null);
        ReviewResult review = reviewAdapter.reviewDocument("project-001", new PhotoInfoResult(2, "bundle.json"), style);

        assertThat(draft.draftSectionCount()).isEqualTo(2);
        assertThat(draft.resultPath()).isEqualTo("output/project-001/draft/draft.json");
        assertThat(style.wordCount()).isEqualTo(120);
        assertThat(style.resultPath()).isEqualTo("output/project-001/style/styled.json");
        assertThat(review.issueCount()).isZero();
        assertThat(review.resultPath()).isEqualTo("output/project-001/review/final.json");

        assertThat(Files.exists(Path.of(draft.resultPath()))).isTrue();
        assertThat(Files.readString(Path.of(draft.resultPath()))).contains("\"artifact_type\": \"draft_result\"");
        assertThat(Files.exists(Path.of(style.resultPath()))).isTrue();
        assertThat(Files.readString(Path.of(style.resultPath()))).contains("\"artifact_type\": \"style_result\"");
        assertThat(Files.exists(Path.of(review.resultPath()))).isTrue();
        assertThat(Files.readString(Path.of(review.resultPath()))).contains("\"artifact_type\": \"review_result\"");
    }
}
