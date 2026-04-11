package com.momently.orchestrator.adapter.in.web.response;

import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.UUID;

/**
 * Response payload describing a workflow resource.
 *
 * @param workflowId workflow identifier
 * @param projectId project identifier
 * @param groupingStrategy selected grouping strategy
 * @param status current workflow status
 */
public record WorkflowResponse(
    UUID workflowId,
    String projectId,
    String groupingStrategy,
    WorkflowStatus status
) {
}
