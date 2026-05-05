package com.momently.orchestrator.adapter.in.web;

import com.momently.orchestrator.domain.Workflow;
import java.util.UUID;

/**
 * SSE로 내려보내는 워크플로 상태 이벤트 본문이다.
 */
record WorkflowEventPayload(
    UUID workflowId,
    String projectId,
    String status,
    Integer photoCount,
    Integer privacyExcludedCount,
    Double averageQualityScore,
    Integer groupCount,
    Integer heroPhotoCount,
    Integer outlineSectionCount,
    Integer draftSectionCount,
    Integer styledWordCount,
    Integer reviewIssueCount,
    String lastFailedStep,
    String lastErrorMessage
) {

    static WorkflowEventPayload from(Workflow workflow) {
        return new WorkflowEventPayload(
            workflow.getWorkflowId(),
            workflow.getProjectId(),
            workflow.getStatus().name(),
            workflow.getPhotoCount(),
            workflow.getPrivacyExcludedCount(),
            workflow.getAverageQualityScore(),
            workflow.getGroupCount(),
            workflow.getHeroPhotoCount(),
            workflow.getOutlineSectionCount(),
            workflow.getDraftSectionCount(),
            workflow.getStyledWordCount(),
            workflow.getReviewIssueCount(),
            workflow.getLastFailedStep(),
            workflow.getLastErrorMessage()
        );
    }
}
