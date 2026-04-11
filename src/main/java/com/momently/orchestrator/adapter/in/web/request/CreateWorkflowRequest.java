package com.momently.orchestrator.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for creating a workflow.
 *
 * @param projectId project identifier
 * @param groupingStrategy selected grouping strategy
 */
public record CreateWorkflowRequest(
    @NotBlank String projectId,
    @NotBlank String groupingStrategy
) {
}
