package com.momently.orchestrator.adapter.out.persistence;

import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflows")
class WorkflowJpaEntity {

    @Id
    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "grouping_strategy", nullable = false)
    private String groupingStrategy;

    @Column(name = "time_window_minutes", nullable = false)
    private int timeWindowMinutes;

    @Column(name = "voice_profile_id")
    private String voiceProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkflowStatus status;

    @Column(name = "last_failed_step")
    private String lastFailedStep;

    @Column(name = "last_error_message", columnDefinition = "text")
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkflowJpaEntity() {
    }

    static WorkflowJpaEntity fromDomain(Workflow workflow) {
        WorkflowJpaEntity entity = new WorkflowJpaEntity();
        entity.workflowId = workflow.getWorkflowId();
        entity.createdAt = Instant.now();
        entity.updateFrom(workflow);
        return entity;
    }

    void updateFrom(Workflow workflow) {
        this.projectId = workflow.getProjectId();
        this.groupingStrategy = workflow.getGroupingStrategy();
        this.timeWindowMinutes = workflow.getTimeWindowMinutes();
        this.voiceProfileId = workflow.getVoiceProfileId();
        this.status = workflow.getStatus();
        this.lastFailedStep = workflow.getLastFailedStep();
        this.lastErrorMessage = workflow.getLastErrorMessage();
        this.photoCount = workflow.getPhotoCount();
        this.privacyExcludedCount = workflow.getPrivacyExcludedCount();
        this.averageQualityScore = workflow.getAverageQualityScore();
        this.groupCount = workflow.getGroupCount();
        this.heroPhotoCount = workflow.getHeroPhotoCount();
        this.outlineSectionCount = workflow.getOutlineSectionCount();
        this.draftSectionCount = workflow.getDraftSectionCount();
        this.styledWordCount = workflow.getStyledWordCount();
        this.reviewIssueCount = workflow.getReviewIssueCount();
        this.photoInfoBundlePath = workflow.getPhotoInfoBundlePath();
        this.privacyResultPath = workflow.getPrivacyResultPath();
        this.privacyBundlePath = workflow.getPrivacyBundlePath();
        this.qualityScoreResultPath = workflow.getQualityScoreResultPath();
        this.qualityScoreBundlePath = workflow.getQualityScoreBundlePath();
        this.blogPath = workflow.getBlogPath();
        this.groupingResultPath = workflow.getGroupingResultPath();
        this.heroPhotoResultPath = workflow.getHeroPhotoResultPath();
        this.outlineResultPath = workflow.getOutlineResultPath();
        this.draftResultPath = workflow.getDraftResultPath();
        this.styleResultPath = workflow.getStyleResultPath();
        this.reviewResultPath = workflow.getReviewResultPath();
    }

    Workflow toDomain() {
        Workflow workflow = new Workflow(
            workflowId,
            projectId,
            groupingStrategy,
            timeWindowMinutes,
            voiceProfileId,
            status
        );
        if (photoCount != null || photoInfoBundlePath != null || blogPath != null) {
            workflow.recordPhotoInfoArtifacts(valueOrZero(photoCount), photoInfoBundlePath, blogPath);
        }
        if (privacyExcludedCount != null || privacyResultPath != null || privacyBundlePath != null) {
            workflow.recordPrivacyArtifacts(valueOrZero(photoCount), valueOrZero(privacyExcludedCount), privacyResultPath, privacyBundlePath);
        }
        if (averageQualityScore != null || qualityScoreResultPath != null || qualityScoreBundlePath != null) {
            workflow.recordQualityScoreArtifacts(valueOrZero(photoCount), averageQualityScore == null ? 0.0 : averageQualityScore, qualityScoreResultPath, qualityScoreBundlePath);
        }
        if (groupCount != null || groupingResultPath != null) {
            workflow.recordGroupingArtifacts(valueOrZero(groupCount), groupingResultPath);
        }
        if (heroPhotoCount != null || heroPhotoResultPath != null) {
            workflow.recordHeroPhotoArtifacts(valueOrZero(heroPhotoCount), heroPhotoResultPath);
        }
        if (outlineSectionCount != null || outlineResultPath != null) {
            workflow.recordOutlineArtifacts(valueOrZero(outlineSectionCount), outlineResultPath);
        }
        if (draftSectionCount != null || draftResultPath != null) {
            workflow.recordDraftArtifacts(valueOrZero(draftSectionCount), draftResultPath);
        }
        if (styledWordCount != null || styleResultPath != null) {
            workflow.recordStyleArtifacts(valueOrZero(styledWordCount), styleResultPath);
        }
        if (reviewIssueCount != null || reviewResultPath != null) {
            workflow.recordReviewArtifacts(valueOrZero(reviewIssueCount), reviewResultPath);
        }
        if (status == WorkflowStatus.FAILED) {
            workflow.markFailed(lastFailedStep, lastErrorMessage);
        }
        return workflow;
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
