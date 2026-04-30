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
 * @param photoCount number of public photos available for downstream steps
 * @param groupCount number of groups produced by the grouping agent
 * @param heroPhotoCount number of selected hero photos produced by the hero photo agent
 * @param outlineSectionCount number of sections produced by the outline agent
 * @param draftSectionCount number of draft sections produced by the draft agent
 * @param styledWordCount word count after styling
 * @param reviewIssueCount number of issues found by the review agent
 * @param photoInfoBundlePath path to the generated photo information bundle artifact
 * @param blogPath path to the generated blog Markdown artifact, or null when skipped
 * @param groupingResultPath path to the generated grouping result artifact
 * @param heroPhotoResultPath path to the generated hero photo selection result artifact
 * @param outlineResultPath path to the generated outline result artifact
 * @param draftResultPath path to the generated draft result artifact
 * @param styleResultPath path to the generated styled result artifact
 * @param reviewResultPath path to the generated final review artifact
 * @param lastFailedStep step name where the latest failure happened, or null
 * @param lastErrorMessage failure message for the latest failed step, or null
 */
public record WorkflowResponse(
    UUID workflowId,
    String projectId,
    String groupingStrategy,
    WorkflowStatus status,
    Integer photoCount,
    Integer privacyExcludedCount,
    Integer groupCount,
    Integer heroPhotoCount,
    Integer outlineSectionCount,
    Integer draftSectionCount,
    Integer styledWordCount,
    Integer reviewIssueCount,
    String photoInfoBundlePath,
    String privacyResultPath,
    String privacyBundlePath,
    String blogPath,
    String groupingResultPath,
    String heroPhotoResultPath,
    String outlineResultPath,
    String draftResultPath,
    String styleResultPath,
    String reviewResultPath,
    String lastFailedStep,
    String lastErrorMessage
) {
}
