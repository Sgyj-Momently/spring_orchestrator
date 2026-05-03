package com.momently.orchestrator.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * 단일 워크플로 실행을 나타내는 도메인 집합체(aggregate)다.
 */
public class Workflow {

    private final UUID workflowId;
    private final String projectId;
    private final String groupingStrategy;
    private final int timeWindowMinutes;
    private final String voiceProfileId;
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
     * 새 워크플로 집합체를 생성한다.
     *
     * @param workflowId 워크플로 식별자
     * @param projectId 프로젝트 식별자
     * @param groupingStrategy 선택된 그룹화 전략
     * @param timeWindowMinutes 같은 이벤트를 묶을 최대 시간 간격(분)
     * @param status 초기 워크플로 상태
     */
    public Workflow(UUID workflowId, String projectId, String groupingStrategy, int timeWindowMinutes, WorkflowStatus status) {
        this(workflowId, projectId, groupingStrategy, timeWindowMinutes, null, status);
    }

    public Workflow(
        UUID workflowId,
        String projectId,
        String groupingStrategy,
        int timeWindowMinutes,
        String voiceProfileId,
        WorkflowStatus status
    ) {
        this.workflowId = Objects.requireNonNull(workflowId);
        this.projectId = Objects.requireNonNull(projectId);
        this.groupingStrategy = Objects.requireNonNull(groupingStrategy);
        this.timeWindowMinutes = timeWindowMinutes;
        this.voiceProfileId = voiceProfileId;
        this.status = Objects.requireNonNull(status);
    }

    /**
     * 워크플로 상태를 갱신한다.
     *
     * @param status 다음 워크플로 상태
     */
    public void updateStatus(WorkflowStatus status) {
        this.status = Objects.requireNonNull(status);
    }

    /**
     * 워크플로를 실패로 표시하고 실패 메타데이터를 기록한다.
     *
     * @param failedStep 실패가 난 단계 이름
     * @param errorMessage 실패 원인 설명 메시지
     */
    public void markFailed(String failedStep, String errorMessage) {
        this.status = WorkflowStatus.FAILED;
        this.lastFailedStep = failedStep;
        this.lastErrorMessage = errorMessage;
    }

    /**
     * 사진 정보 추출 단계에서 생성된 아티팩트를 기록한다.
     *
     * <p>워크플로 집합체에는 아티팩트 참조 문자열과 집계 수만 둔다. 큰 JSON·마크다운 산출물은 디스크에 두고
     * 이후 단계가 도메인 모델에 무거운 본문을 올리지 않고 경로로 재사용한다.</p>
     *
     * @param photoCount 이후 단계에 사용할 공개 사진 수
     * @param bundlePath 생성된 bundle JSON 아티팩트 경로
     * @param blogPath 생성된 블로그 마크다운 경로, 생략 시 null
     */
    public void recordPhotoInfoArtifacts(int photoCount, String bundlePath, String blogPath) {
        this.photoCount = photoCount;
        this.photoInfoBundlePath = bundlePath;
        this.blogPath = blogPath;
    }

    /**
     * 민감정보 안전성 단계에서 생성된 아티팩트를 기록한다.
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
     * 사진 품질 점수 단계에서 생성된 아티팩트를 기록한다.
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
     * 사진 그룹화 단계에서 생성된 아티팩트를 기록한다.
     *
     * @param groupCount 그룹화 에이전트가 만든 그룹 수
     * @param groupingResultPath 그룹화 결과 JSON 아티팩트 경로
     */
    public void recordGroupingArtifacts(int groupCount, String groupingResultPath) {
        this.groupCount = groupCount;
        this.groupingResultPath = groupingResultPath;
    }

    /**
     * 대표 사진 선택 단계에서 생성된 아티팩트를 기록한다.
     *
     * @param heroPhotoCount 선택된 대표 사진 수
     * @param heroPhotoResultPath 대표 사진 선택 결과 JSON 아티팩트 경로
     */
    public void recordHeroPhotoArtifacts(int heroPhotoCount, String heroPhotoResultPath) {
        this.heroPhotoCount = heroPhotoCount;
        this.heroPhotoResultPath = heroPhotoResultPath;
    }

    /**
     * 개요(outline) 단계에서 생성된 아티팩트를 기록한다.
     *
     * @param outlineSectionCount 개요 섹션 수
     * @param outlineResultPath 개요 JSON 아티팩트 경로
     */
    public void recordOutlineArtifacts(int outlineSectionCount, String outlineResultPath) {
        this.outlineSectionCount = outlineSectionCount;
        this.outlineResultPath = outlineResultPath;
    }

    /**
     * 초안(draft) 단계에서 생성된 아티팩트를 기록한다.
     */
    public void recordDraftArtifacts(int draftSectionCount, String draftResultPath) {
        this.draftSectionCount = draftSectionCount;
        this.draftResultPath = draftResultPath;
    }

    /**
     * 문체(style) 적용 단계에서 생성된 아티팩트를 기록한다.
     */
    public void recordStyleArtifacts(int styledWordCount, String styleResultPath) {
        this.styledWordCount = styledWordCount;
        this.styleResultPath = styleResultPath;
    }

    /**
     * 검수(review) 단계에서 생성된 아티팩트를 기록한다.
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

    public String getVoiceProfileId() {
        return voiceProfileId;
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
