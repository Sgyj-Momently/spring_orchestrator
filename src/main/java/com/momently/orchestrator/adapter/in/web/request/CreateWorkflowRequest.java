package com.momently.orchestrator.adapter.in.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for creating a workflow.
 *
 * @param projectId project identifier
 * @param groupingStrategy selected grouping strategy
 * @param timeWindowMinutes maximum time gap in minutes for grouping the same event; null defaults to 90
 */
public record CreateWorkflowRequest(
    @NotBlank String projectId,
    @NotBlank String groupingStrategy,
    @Min(1) Integer timeWindowMinutes
) {

    private static final int DEFAULT_TIME_WINDOW_MINUTES = 90;

    /**
     * timeWindowMinutes가 지정되지 않으면 기본값 90분을 반환한다.
     */
    public int resolvedTimeWindowMinutes() {
        return timeWindowMinutes != null ? timeWindowMinutes : DEFAULT_TIME_WINDOW_MINUTES;
    }
}
