package com.momently.orchestrator.application.port.in;

import com.momently.orchestrator.application.port.in.command.CreateWorkflowCommand;
import com.momently.orchestrator.domain.Workflow;

/**
 * 새 워크플로를 시작하는 inbound 유스케이스다.
 */
public interface CreateWorkflowUseCase {

    /**
     * 워크플로를 만들고 저장된 집합체를 반환한다.
     *
     * @param command 워크플로 생성 명령
     * @return 생성된 워크플로 집합체
     */
    Workflow createWorkflow(CreateWorkflowCommand command);
}
