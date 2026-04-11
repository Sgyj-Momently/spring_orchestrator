package com.momently.orchestrator.application.port.in;

import com.momently.orchestrator.domain.Workflow;
import java.util.UUID;

/**
 * Use case for retrieving a workflow.
 */
public interface GetWorkflowUseCase {

    /**
     * Loads a workflow by id.
     *
     * @param workflowId workflow identifier
     * @return workflow aggregate
     */
    Workflow getWorkflow(UUID workflowId);
}
