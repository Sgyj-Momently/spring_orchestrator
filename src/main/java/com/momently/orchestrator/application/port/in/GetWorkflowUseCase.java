package com.momently.orchestrator.application.port.in;

import com.momently.orchestrator.domain.Workflow;
import java.util.List;
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

    /**
     * 저장된 워크플로 목록을 최근 작업 순으로 조회한다.
     *
     * @return 워크플로 목록
     */
    List<Workflow> listWorkflows();

    /**
     * 저장된 워크플로 기록을 모두 삭제한다.
     */
    void deleteAllWorkflows();
}
