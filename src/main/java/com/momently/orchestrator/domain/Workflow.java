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
    private final int timeWindowMinutes;
    private WorkflowStatus status;
    private String lastFailedStep;
    private String lastErrorMessage;
    private Integer photoCount;
    private Integer privacyExcludedCount;
    private Double averageQualityScore;
    private Integer groupCount;
    private Integer heroPhotoCount;
    private Integer outlineSectionCount;
    private Integer draftSectionCount;
    private Integer styledWordCount;
    private Integer reviewIssueCount;
    private String photoInfoBundlePath;
    private String privacyResultPath;
    private String privacyBundlePath;
    private String qualityScoreResultPath;
    private String qualityScoreBundlePath;
    private String blogPath;
    private String groupingResultPath;
    private String heroPhotoResultPath;
    private String outlineResultPath;
    private String draftResultPath;
    private String styleResultPath;
    private String reviewResultPath;

    /**
     * Creates a new workflow aggregate.
     *
     * @param workflowId workflow identifier
     * @param projectId project identifier
     * @param groupingStrategy selected grouping strategy
     * @param timeWindowMinutes maximum time gap in minutes for grouping the same event
     * @param status initial workflow status
     */
    public Workflow(UUID workflowId, String projectId, String groupingStrategy, int timeWindowMinutes, WorkflowStatus status) {
        this.workflowId = Objects.requireNonNull(workflowId);
        this.projectId = Objects.requireNonNull(projectId);
        this.groupingStrategy = Objects.requireNonNull(groupingStrategy);
        this.timeWindowMinutes = timeWindowMinutes;
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

    /**
     * Records artifacts produced by the photo information step.
     *
     * <p>The workflow aggregate keeps only small artifact references and counters. Large JSON and Markdown
     * outputs stay on disk so later steps can reuse them without loading bulky content into the domain model.</p>
     *
     * @param photoCount number of public photos available for later steps
     * @param bundlePath path to the generated bundle JSON artifact
     * @param blogPath path to the generated blog Markdown artifact, or null when skipped
     */
    public void recordPhotoInfoArtifacts(int photoCount, String bundlePath, String blogPath) {
        this.photoCount = photoCount;
        this.photoInfoBundlePath = bundlePath;
        this.blogPath = blogPath;
    }

    /**
     * Records artifacts produced by the privacy safety step.
     */
    public void recordPrivacyArtifacts(
        int publicPhotoCount,
        int excludedPhotoCount,
        String privacyResultPath,
        String privacyBundlePath
    ) {
        this.photoCount = publicPhotoCount;
        this.privacyExcludedCount = excludedPhotoCount;
        this.privacyResultPath = privacyResultPath;
        this.privacyBundlePath = privacyBundlePath;
    }

    /**
     * Records artifacts produced by the quality score step.
     */
    public void recordQualityScoreArtifacts(
        int scoredPhotoCount,
        double averageQualityScore,
        String qualityScoreResultPath,
        String qualityScoreBundlePath
    ) {
        this.photoCount = scoredPhotoCount;
        this.averageQualityScore = averageQualityScore;
        this.qualityScoreResultPath = qualityScoreResultPath;
        this.qualityScoreBundlePath = qualityScoreBundlePath;
    }

    /**
     * Records artifacts produced by the photo grouping step.
     *
     * @param groupCount number of groups produced by the grouping agent
     * @param groupingResultPath path to the grouping result JSON artifact
     */
    public void recordGroupingArtifacts(int groupCount, String groupingResultPath) {
        this.groupCount = groupCount;
        this.groupingResultPath = groupingResultPath;
    }

    /**
     * Records artifacts produced by the hero photo selection step.
     *
     * @param heroPhotoCount number of selected hero photos
     * @param heroPhotoResultPath path to the hero photo selection result JSON artifact
     */
    public void recordHeroPhotoArtifacts(int heroPhotoCount, String heroPhotoResultPath) {
        this.heroPhotoCount = heroPhotoCount;
        this.heroPhotoResultPath = heroPhotoResultPath;
    }

    /**
     * Records artifacts produced by the outline step.
     *
     * @param outlineSectionCount number of outline sections
     * @param outlineResultPath path to the outline JSON artifact
     */
    public void recordOutlineArtifacts(int outlineSectionCount, String outlineResultPath) {
        this.outlineSectionCount = outlineSectionCount;
        this.outlineResultPath = outlineResultPath;
    }

    /**
     * Records artifacts produced by the draft step.
     */
    public void recordDraftArtifacts(int draftSectionCount, String draftResultPath) {
        this.draftSectionCount = draftSectionCount;
        this.draftResultPath = draftResultPath;
    }

    /**
     * Records artifacts produced by the style step.
     */
    public void recordStyleArtifacts(int styledWordCount, String styleResultPath) {
        this.styledWordCount = styledWordCount;
        this.styleResultPath = styleResultPath;
    }

    /**
     * Records artifacts produced by the review step.
     */
    public void recordReviewArtifacts(int reviewIssueCount, String reviewResultPath) {
        this.reviewIssueCount = reviewIssueCount;
        this.reviewResultPath = reviewResultPath;
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

    public int getTimeWindowMinutes() {
        return timeWindowMinutes;
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

    public Integer getPhotoCount() {
        return photoCount;
    }

    public Integer getPrivacyExcludedCount() {
        return privacyExcludedCount;
    }

    public Double getAverageQualityScore() {
        return averageQualityScore;
    }

    public Integer getGroupCount() {
        return groupCount;
    }

    public Integer getHeroPhotoCount() {
        return heroPhotoCount;
    }

    public Integer getOutlineSectionCount() {
        return outlineSectionCount;
    }

    public Integer getDraftSectionCount() {
        return draftSectionCount;
    }

    public Integer getStyledWordCount() {
        return styledWordCount;
    }

    public Integer getReviewIssueCount() {
        return reviewIssueCount;
    }

    public String getPhotoInfoBundlePath() {
        return photoInfoBundlePath;
    }

    public String getPrivacyResultPath() {
        return privacyResultPath;
    }

    public String getPrivacyBundlePath() {
        return privacyBundlePath;
    }

    public String getQualityScoreResultPath() {
        return qualityScoreResultPath;
    }

    public String getQualityScoreBundlePath() {
        return qualityScoreBundlePath;
    }

    public String getBlogPath() {
        return blogPath;
    }

    public String getGroupingResultPath() {
        return groupingResultPath;
    }

    public String getHeroPhotoResultPath() {
        return heroPhotoResultPath;
    }

    public String getOutlineResultPath() {
        return outlineResultPath;
    }

    public String getDraftResultPath() {
        return draftResultPath;
    }

    public String getStyleResultPath() {
        return styleResultPath;
    }

    public String getReviewResultPath() {
        return reviewResultPath;
    }
}
