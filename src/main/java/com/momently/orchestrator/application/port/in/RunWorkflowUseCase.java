package com.momently.orchestrator.application.port.in;

import com.momently.orchestrator.domain.Workflow;
import java.util.UUID;

/**
 * 워크플로를 정의된 순서대로 실행하는 유스케이스다.
 */
public interface RunWorkflowUseCase {

    /**
     * 지정한 워크플로를 처음부터 다음 단계들로 순차 실행한다.
     *
     * @param workflowId 워크플로 식별자
     * @return 실행 결과가 반영된 워크플로
     */
    Workflow runWorkflow(UUID workflowId);
}
