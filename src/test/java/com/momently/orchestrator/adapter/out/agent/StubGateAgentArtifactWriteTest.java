package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.PrivacySafetyResult;
import com.momently.orchestrator.application.port.out.result.QualityScoreResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StubGateAgentArtifactWriteTest {

    @Test
    @DisplayName("privacy/quality stub adapters persist result artifacts for artifact endpoint")
    void persistsPrivacyAndQualityArtifacts() throws IOException {
        StubPrivacySafetyAgentAdapter privacyAdapter = new StubPrivacySafetyAgentAdapter();
        StubQualityScoreAgentAdapter qualityAdapter = new StubQualityScoreAgentAdapter();

        PhotoInfoResult photoInfo = new PhotoInfoResult(0, "artifacts/photo-info/project-001/bundle.json");
        PrivacySafetyResult privacy = privacyAdapter.reviewPrivacy("project-001", photoInfo);
        QualityScoreResult quality = qualityAdapter.scorePhotos("project-001", photoInfo);

        assertThat(privacy.resultPath()).isEqualTo("output/project-001/privacy/privacy-result.json");
        assertThat(quality.resultPath()).isEqualTo("output/project-001/quality/quality-result.json");

        Path privacyPath = Path.of(privacy.resultPath());
        Path qualityPath = Path.of(quality.resultPath());

        assertThat(Files.exists(privacyPath)).isTrue();
        assertThat(Files.readString(privacyPath)).contains("\"artifact_type\": \"privacy_safety_result\"");

        assertThat(Files.exists(qualityPath)).isTrue();
        assertThat(Files.readString(qualityPath)).contains("\"artifact_type\": \"quality_score_result\"");
    }
}

