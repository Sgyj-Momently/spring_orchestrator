package com.momently.orchestrator.adapter.in.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 워크플로 생성 요청 본문이다.
 *
 * @param projectId 프로젝트 식별자
 * @param groupingStrategy 선택한 그룹화 전략
 * @param timeWindowMinutes 같은 이벤트를 묶을 최대 간격(분), null이면 90분 기본값
 * @param voiceProfileId 말투 프로필 식별자, null이면 기본 문체
 * @param contentType 사용자가 선택한 글 종류
 * @param writingInstructions 사용자가 추가로 입력한 작성 방향
 */
public record CreateWorkflowRequest(
    @NotBlank String projectId,
    @NotBlank String groupingStrategy,
    @Min(1) Integer timeWindowMinutes,
    String voiceProfileId,
    String contentType,
    String writingInstructions
) {

    private static final int DEFAULT_TIME_WINDOW_MINUTES = 90;

    public CreateWorkflowRequest(String projectId, String groupingStrategy, Integer timeWindowMinutes) {
        this(projectId, groupingStrategy, timeWindowMinutes, null, null, null);
    }

    /**
     * timeWindowMinutes가 지정되지 않으면 기본값 90분을 반환한다.
     */
    public int resolvedTimeWindowMinutes() {
        return timeWindowMinutes != null ? timeWindowMinutes : DEFAULT_TIME_WINDOW_MINUTES;
    }
}
