package com.momently.orchestrator.application.port.in;

import java.util.UUID;

/**
 * 워크플로를 정의된 순서대로 실행하는 유스케이스다.
 */
public interface RunWorkflowUseCase {

    /**
     * 지정한 워크플로를 비동기로 실행한다. 호출 즉시 반환하며 실행 결과는 워크플로 상태로 확인한다.
     *
     * @param workflowId 워크플로 식별자
     */
    void runWorkflow(UUID workflowId);
}
