package com.momently.orchestrator.application.port.in;

import com.momently.orchestrator.application.port.in.command.CreateWorkflowCommand;
import com.momently.orchestrator.domain.Workflow;

/**
 * Use case for starting a new workflow.
 */
public interface CreateWorkflowUseCase {

    /**
     * Creates a workflow and returns the created aggregate.
     *
     * @param command workflow creation command
     * @return created workflow aggregate
     */
    Workflow createWorkflow(CreateWorkflowCommand command);
}
