package com.momently.orchestrator.application.port.in;

import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.UUID;

/**
 * 워크플로 상태를 다음 단계로 전이시키는 유스케이스다.
 */
public interface AdvanceWorkflowUseCase {

    /**
     * 워크플로를 지정한 다음 상태로 전이시킨다.
     *
     * @param workflowId 워크플로 식별자
     * @param nextStatus 전이할 다음 상태
     * @return 상태가 반영된 워크플로
     */
    Workflow advanceWorkflow(UUID workflowId, WorkflowStatus nextStatus);
}
