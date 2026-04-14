package com.momently.orchestrator.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 워크플로 도메인 객체의 기본 동작을 검증한다.
 */
class WorkflowTest {

    @Test
    @DisplayName("워크플로 생성 시 전달한 값들을 그대로 보존한다")
    void createsWorkflowWithGivenValues() {
        UUID workflowId = UUID.randomUUID();

        Workflow workflow = new Workflow(
            workflowId,
            "project-001",
            "LOCATION_BASED",
            WorkflowStatus.CREATED
        );

        assertThat(workflow.getWorkflowId()).isEqualTo(workflowId);
        assertThat(workflow.getProjectId()).isEqualTo("project-001");
        assertThat(workflow.getGroupingStrategy()).isEqualTo("LOCATION_BASED");
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.CREATED);
    }

    @Test
    @DisplayName("상태를 갱신할 수 있다")
    void updatesStatus() {
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            WorkflowStatus.CREATED
        );

        workflow.updateStatus(WorkflowStatus.PHOTO_INFO_EXTRACTING);

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.PHOTO_INFO_EXTRACTING);
    }

    @Test
    @DisplayName("실패 처리 시 실패 메타데이터를 기록한다")
    void marksFailed() {
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            WorkflowStatus.PHOTO_GROUPING
        );

        workflow.markFailed("PHOTO_GROUPING", "timeout");

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(workflow.getLastFailedStep()).isEqualTo("PHOTO_GROUPING");
        assertThat(workflow.getLastErrorMessage()).isEqualTo("timeout");
    }

    @Test
    @DisplayName("필수 값이 없으면 생성할 수 없다")
    void rejectsNullRequiredValues() {
        assertThatThrownBy(() -> new Workflow(
            null,
            "project-001",
            "LOCATION_BASED",
            WorkflowStatus.CREATED
        )).isInstanceOf(NullPointerException.class);
    }
}
