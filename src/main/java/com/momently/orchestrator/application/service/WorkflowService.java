package com.momently.orchestrator.application.service;

import com.momently.orchestrator.application.port.in.AdvanceWorkflowUseCase;
import com.momently.orchestrator.application.port.in.CreateWorkflowUseCase;
import com.momently.orchestrator.application.port.in.GetWorkflowUseCase;
import com.momently.orchestrator.application.port.in.command.CreateWorkflowCommand;
import com.momently.orchestrator.application.port.out.WorkflowRepository;
import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 워크플로 생성, 조회, 상태 전이를 담당하는 애플리케이션 서비스다.
 */
@Service
public class WorkflowService implements CreateWorkflowUseCase, GetWorkflowUseCase, AdvanceWorkflowUseCase {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStateMachine workflowStateMachine;

    /**
     * 워크플로 서비스 의존성을 생성한다.
     *
     * @param workflowRepository 워크플로 저장 포트
     * @param workflowStateMachine 워크플로 상태 머신
     */
    public WorkflowService(
        WorkflowRepository workflowRepository,
        WorkflowStateMachine workflowStateMachine
    ) {
        this.workflowRepository = workflowRepository;
        this.workflowStateMachine = workflowStateMachine;
    }

    @Override
    public Workflow createWorkflow(CreateWorkflowCommand command) {
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            command.projectId(),
            command.groupingStrategy(),
            command.timeWindowMinutes(),
            command.voiceProfileId(),
            WorkflowStatus.CREATED
        );
        return workflowRepository.save(workflow);
    }

    @Override
    public Workflow getWorkflow(UUID workflowId) {
        return workflowRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));
    }

    @Override
    public Workflow advanceWorkflow(UUID workflowId, WorkflowStatus nextStatus) {
        Workflow workflow = getWorkflow(workflowId);
        workflowStateMachine.transition(workflow, nextStatus);
        return workflowRepository.save(workflow);
    }
}
