package com.momently.orchestrator.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Domain aggregate representing a single workflow execution.
 */
public class Workflow {

    private final UUID workflowId;
    private final String projectId;
    private final String groupingStrategy;
    private WorkflowStatus status;
    private String lastFailedStep;
    private String lastErrorMessage;

    /**
     * Creates a new workflow aggregate.
     *
     * @param workflowId workflow identifier
     * @param projectId project identifier
     * @param groupingStrategy selected grouping strategy
     * @param status initial workflow status
     */
    public Workflow(UUID workflowId, String projectId, String groupingStrategy, WorkflowStatus status) {
        this.workflowId = Objects.requireNonNull(workflowId);
        this.projectId = Objects.requireNonNull(projectId);
        this.groupingStrategy = Objects.requireNonNull(groupingStrategy);
        this.status = Objects.requireNonNull(status);
    }

    /**
     * Updates the workflow status.
     *
     * @param status next workflow status
     */
    public void updateStatus(WorkflowStatus status) {
        this.status = Objects.requireNonNull(status);
    }

    /**
     * Marks the workflow as failed and records failure metadata.
     *
     * @param failedStep step name where the failure happened
     * @param errorMessage message explaining the failure
     */
    public void markFailed(String failedStep, String errorMessage) {
        this.status = WorkflowStatus.FAILED;
        this.lastFailedStep = failedStep;
        this.lastErrorMessage = errorMessage;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getGroupingStrategy() {
        return groupingStrategy;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public String getLastFailedStep() {
        return lastFailedStep;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }
}
