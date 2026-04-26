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
    private Integer groupCount;
    private Integer heroPhotoCount;
    private String photoInfoBundlePath;
    private String blogPath;
    private String groupingResultPath;
    private String heroPhotoResultPath;

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

    public Integer getGroupCount() {
        return groupCount;
    }

    public Integer getHeroPhotoCount() {
        return heroPhotoCount;
    }

    public String getPhotoInfoBundlePath() {
        return photoInfoBundlePath;
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
}
