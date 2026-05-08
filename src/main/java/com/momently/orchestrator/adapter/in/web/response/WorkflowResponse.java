package com.momently.orchestrator.adapter.in.web.response;

import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.UUID;

/**
 * 워크플로 리소스를 설명하는 HTTP 응답 본문이다.
 *
 * @param workflowId 워크플로 식별자
 * @param projectId 프로젝트 식별자
 * @param groupingStrategy 선택한 그룹화 전략
 * @param contentType 사용자가 선택한 글 종류
 * @param writingInstructions 사용자가 추가로 입력한 작성 방향
 * @param status 현재 워크플로 상태
 * @param photoCount 이후 단계에 쓸 수 있는 공개 사진 수
 * @param privacyExcludedCount 민감정보 단계에서 제외한 사진 수
 * @param averageQualityScore 품질 점수 단계 이후 평균 품질 점수
 * @param groupCount 그룹화 에이전트가 만든 그룹 수
 * @param heroPhotoCount 대표 사진 에이전트가 고른 대표 사진 수
 * @param outlineSectionCount 개요 에이전트가 만든 섹션 수
 * @param draftSectionCount 초안 에이전트가 만든 초안 섹션 수
 * @param styledWordCount 문체 적용 후 단어 수
 * @param reviewIssueCount 검수 에이전트가 찾은 이슈 수
 * @param photoInfoBundlePath 사진 정보 bundle JSON 아티팩트 경로
 * @param privacyResultPath 민감정보 검사 결과 JSON 아티팩트 경로
 * @param privacyBundlePath 공개용 사진 bundle JSON 아티팩트 경로
 * @param qualityScoreResultPath 품질 점수 결과 JSON 아티팩트 경로
 * @param qualityScoreBundlePath 점수 반영된 bundle JSON 아티팩트 경로
 * @param blogPath 블로그 마크다운 아티팩트 경로, 생략 시 null
 * @param groupingResultPath 그룹화 결과 JSON 아티팩트 경로
 * @param heroPhotoResultPath 대표 사진 선택 결과 JSON 아티팩트 경로
 * @param outlineResultPath 개요 JSON 아티팩트 경로
 * @param draftResultPath 초안 JSON 아티팩트 경로
 * @param styleResultPath 문체 적용 결과 JSON 아티팩트 경로
 * @param reviewResultPath 최종 검수 JSON 아티팩트 경로
 * @param lastFailedStep 가장 마지막 실패 단계 이름, 없으면 null
 * @param lastErrorMessage 가장 마지막 실패 메시지, 없으면 null
 */
public record WorkflowResponse(
    UUID workflowId,
    String projectId,
    String groupingStrategy,
    String contentType,
    String writingInstructions,
    WorkflowStatus status,
    Integer photoCount,
    Integer privacyExcludedCount,
    Double averageQualityScore,
    Integer groupCount,
    Integer heroPhotoCount,
    Integer outlineSectionCount,
    Integer draftSectionCount,
    Integer styledWordCount,
    Integer reviewIssueCount,
    String photoInfoBundlePath,
    String privacyResultPath,
    String privacyBundlePath,
    String qualityScoreResultPath,
    String qualityScoreBundlePath,
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
    public WorkflowResponse(
        UUID workflowId,
        String projectId,
        String groupingStrategy,
        WorkflowStatus status,
        Integer photoCount,
        Integer privacyExcludedCount,
        Double averageQualityScore,
        Integer groupCount,
        Integer heroPhotoCount,
        Integer outlineSectionCount,
        Integer draftSectionCount,
        Integer styledWordCount,
        Integer reviewIssueCount,
        String photoInfoBundlePath,
        String privacyResultPath,
        String privacyBundlePath,
        String qualityScoreResultPath,
        String qualityScoreBundlePath,
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
        this(
            workflowId,
            projectId,
            groupingStrategy,
            null,
            null,
            status,
            photoCount,
            privacyExcludedCount,
            averageQualityScore,
            groupCount,
            heroPhotoCount,
            outlineSectionCount,
            draftSectionCount,
            styledWordCount,
            reviewIssueCount,
            photoInfoBundlePath,
            privacyResultPath,
            privacyBundlePath,
            qualityScoreResultPath,
            qualityScoreBundlePath,
            blogPath,
            groupingResultPath,
            heroPhotoResultPath,
            outlineResultPath,
            draftResultPath,
            styleResultPath,
            reviewResultPath,
            lastFailedStep,
            lastErrorMessage
        );
    }
}
