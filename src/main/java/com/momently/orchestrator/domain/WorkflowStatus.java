package com.momently.orchestrator.domain;

/**
 * Represents the lifecycle states of a workflow managed by the orchestrator.
 */
public enum WorkflowStatus {
    CREATED,
    PHOTO_INFO_EXTRACTING,
    PHOTO_INFO_EXTRACTED,
    PRIVACY_REVIEWING,
    PRIVACY_REVIEWED,
    QUALITY_SCORING,
    QUALITY_SCORED,
    PHOTO_GROUPING,
    PHOTO_GROUPED,
    HERO_PHOTO_SELECTING,
    HERO_PHOTO_SELECTED,
    OUTLINE_CREATING,
    OUTLINE_CREATED,
    DRAFT_CREATING,
    DRAFT_CREATED,
    STYLE_APPLYING,
    STYLE_APPLIED,
    REVIEWING,
    REVIEW_COMPLETED,
    COMPLETED,
    FAILED
}
