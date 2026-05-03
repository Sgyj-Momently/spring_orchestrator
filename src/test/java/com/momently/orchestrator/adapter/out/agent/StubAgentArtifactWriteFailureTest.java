package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momently.orchestrator.application.port.out.result.DraftResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StubAgentArtifactWriteFailureTest {

    @Test
    @DisplayName("stub adapters는 artifact 쓰기 실패를 IllegalStateException으로 전파한다")
    void propagatesArtifactWriteFailures() throws IOException {
        String projectId = "project-io-error";

        // Files.writeString()이 실패하도록, 파일이 있어야 할 위치를 디렉터리로 미리 만들어 둔다.
        Path stylePath = Path.of("output/%s/style/styled.json".formatted(projectId));
        Files.createDirectories(stylePath);

        StubStyleAgentAdapter styleAdapter = new StubStyleAgentAdapter();
        assertThatThrownBy(() -> styleAdapter.applyStyle(projectId, new DraftResult(1, "output/%s/draft/draft.json".formatted(projectId)), null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to write stub artifact");

        Path reviewPath = Path.of("output/%s/review/final.json".formatted(projectId));
        Files.createDirectories(reviewPath);
        StubReviewAgentAdapter reviewAdapter = new StubReviewAgentAdapter();
        assertThatThrownBy(() -> reviewAdapter.reviewDocument(projectId, new PhotoInfoResult(0, "bundle.json"), new com.momently.orchestrator.application.port.out.result.StyleResult(1, stylePath.toString())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to write stub artifact");

        Path outlinePath = Path.of("output/%s/outline/outline.json".formatted(projectId));
        Files.createDirectories(outlinePath);
        StubOutlineAgentAdapter outlineAdapter = new StubOutlineAgentAdapter();
        assertThatThrownBy(() -> outlineAdapter.createOutline(
            projectId,
            new PhotoInfoResult(0, "bundle.json"),
            new PhotoGroupingResult("TIME_BASED", 0, "grouping.json"),
            new HeroPhotoResult(0, "hero.json")
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to write stub artifact");

        Path privacyPath = Path.of("output/%s/privacy/privacy-result.json".formatted(projectId));
        Files.createDirectories(privacyPath);
        StubPrivacySafetyAgentAdapter privacyAdapter = new StubPrivacySafetyAgentAdapter();
        assertThatThrownBy(() -> privacyAdapter.reviewPrivacy(projectId, new PhotoInfoResult(0, "bundle.json")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to write stub artifact");
    }
}
