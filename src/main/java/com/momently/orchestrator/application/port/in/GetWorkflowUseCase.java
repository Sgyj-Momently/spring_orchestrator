package com.momently.orchestrator.application.port.in;

import com.momently.orchestrator.domain.Workflow;
import java.util.UUID;

/**
 * 워크플로를 조회하는 inbound 유스케이스다.
 */
public interface GetWorkflowUseCase {

    /**
     * 식별자로 워크플로 집합체를 불러온다.
     *
     * @param workflowId 워크플로 식별자
     * @return 워크플로 집합체
     */
    Workflow getWorkflow(UUID workflowId);
}
