package com.momently.orchestrator.application.port.in;

import com.momently.orchestrator.adapter.in.web.request.CreateWorkflowRequest;
import com.momently.orchestrator.domain.Workflow;

/**
 * Use case for starting a new workflow.
 */
public interface CreateWorkflowUseCase {

    /**
     * Creates a workflow and returns the created aggregate.
     *
     * @param request workflow creation request
     * @return created workflow aggregate
     */
    Workflow createWorkflow(CreateWorkflowRequest request);
}
