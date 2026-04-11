package com.momently.orchestrator.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 워크플로 상태 머신 전이 규칙을 검증한다.
 */
class WorkflowStateMachineTest {

    private WorkflowStateMachine workflowStateMachine;

    @BeforeEach
    void setUp() {
        workflowStateMachine = new WorkflowStateMachine();
    }

    @Test
    @DisplayName("문서에 정의된 정상 순차 전이는 허용한다")
    void allowsSequentialTransition() {
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            WorkflowStatus.CREATED
        );

        workflowStateMachine.transition(workflow, WorkflowStatus.PHOTO_INFO_EXTRACTING);

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.PHOTO_INFO_EXTRACTING);
    }

    @Test
    @DisplayName("단계를 건너뛰는 전이는 거부한다")
    void rejectsSkippedTransition() {
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            WorkflowStatus.CREATED
        );

        assertThatThrownBy(() -> workflowStateMachine.transition(workflow, WorkflowStatus.PHOTO_GROUPING))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("허용되지 않은 상태 전이");
    }

    @Test
    @DisplayName("진행 중 상태는 실패 상태로 전이할 수 있다")
    void allowsTransitionToFailed() {
        assertThat(workflowStateMachine.canTransition(WorkflowStatus.PHOTO_GROUPING, WorkflowStatus.FAILED))
            .isTrue();
    }

    @Test
    @DisplayName("완료 상태에서는 실패 상태로 되돌릴 수 없다")
    void rejectsCompletedToFailed() {
        assertThat(workflowStateMachine.canTransition(WorkflowStatus.COMPLETED, WorkflowStatus.FAILED))
            .isFalse();
    }

    @Test
    @DisplayName("실패 이후에는 재시도 가능한 실행 단계로만 복귀할 수 있다")
    void allowsRetryFromFailedOnlyToExecutableStages() {
        assertThat(workflowStateMachine.canTransition(WorkflowStatus.FAILED, WorkflowStatus.PHOTO_GROUPING))
            .isTrue();
        assertThat(workflowStateMachine.canTransition(WorkflowStatus.FAILED, WorkflowStatus.PHOTO_GROUPED))
            .isFalse();
    }
}
