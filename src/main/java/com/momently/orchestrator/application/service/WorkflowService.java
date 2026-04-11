package com.momently.orchestrator.application.service;

import com.momently.orchestrator.adapter.in.web.request.CreateWorkflowRequest;
import com.momently.orchestrator.application.port.in.CreateWorkflowUseCase;
import com.momently.orchestrator.application.port.in.GetWorkflowUseCase;
import com.momently.orchestrator.application.port.out.WorkflowRepository;
import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Application service orchestrating workflow creation and lookup.
 */
@Service
public class WorkflowService implements CreateWorkflowUseCase, GetWorkflowUseCase {

    private final WorkflowRepository workflowRepository;

    /**
     * Creates the workflow service.
     *
     * @param workflowRepository workflow repository port
     */
    public WorkflowService(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @Override
    public Workflow createWorkflow(CreateWorkflowRequest request) {
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            request.projectId(),
            request.groupingStrategy(),
            WorkflowStatus.CREATED
        );
        return workflowRepository.save(workflow);
    }

    @Override
    public Workflow getWorkflow(UUID workflowId) {
        return workflowRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));
    }
}
