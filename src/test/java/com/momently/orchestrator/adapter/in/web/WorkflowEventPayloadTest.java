package com.momently.orchestrator.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkflowEventPayloadTest {

    @Test
    @DisplayName("워크플로 집합체를 SSE 이벤트 본문으로 축약한다")
    void createsPayloadFromWorkflow() {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f41");
        Workflow workflow = new Workflow(
            workflowId,
            "project-events",
            "TIME_BASED",
            90,
            WorkflowStatus.PHOTO_GROUPED
        );
        workflow.recordPhotoInfoArtifacts(5, "bundle.json", null);
        workflow.recordPrivacyArtifacts(4, 1, "privacy.json", "public-bundle.json");
        workflow.recordQualityScoreArtifacts(4, 0.82, "quality.json", "scored-bundle.json");
        workflow.recordGroupingArtifacts(2, "grouping.json");

        WorkflowEventPayload payload = WorkflowEventPayload.from(workflow);

        assertThat(payload.workflowId()).isEqualTo(workflowId);
        assertThat(payload.projectId()).isEqualTo("project-events");
        assertThat(payload.status()).isEqualTo("PHOTO_GROUPED");
        assertThat(payload.photoCount()).isEqualTo(4);
        assertThat(payload.privacyExcludedCount()).isEqualTo(1);
        assertThat(payload.averageQualityScore()).isEqualTo(0.82);
        assertThat(payload.groupCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("실패 메타데이터도 SSE 이벤트 본문에 포함한다")
    void includesFailureMetadata() {
        Workflow workflow = new Workflow(
            UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f42"),
            "project-failed",
            "TIME_BASED",
            90,
            WorkflowStatus.REVIEWING
        );
        workflow.markFailed("REVIEWING", "review timeout");

        WorkflowEventPayload payload = WorkflowEventPayload.from(workflow);

        assertThat(payload.status()).isEqualTo("FAILED");
        assertThat(payload.lastFailedStep()).isEqualTo("REVIEWING");
        assertThat(payload.lastErrorMessage()).isEqualTo("review timeout");
    }
}
