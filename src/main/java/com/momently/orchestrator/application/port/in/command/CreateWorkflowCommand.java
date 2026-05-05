package com.momently.orchestrator.application.port.in.command;

/**
 * 워크플로 생성에 필요한 입력을 담는 애플리케이션 command다.
 *
 * @param projectId 프로젝트 식별자
 * @param groupingStrategy 그룹화 전략
 * @param timeWindowMinutes 같은 그룹으로 묶을 최대 시간 간격(분)
 * @param voiceProfileId 적용할 말투 프로필 식별자
 * @param contentType 사용자가 선택한 글 종류
 * @param writingInstructions 사용자가 추가로 입력한 작성 방향
 */
public record CreateWorkflowCommand(
    String projectId,
    String groupingStrategy,
    int timeWindowMinutes,
    String voiceProfileId,
    String contentType,
    String writingInstructions
) {
    public CreateWorkflowCommand(String projectId, String groupingStrategy, int timeWindowMinutes) {
        this(projectId, groupingStrategy, timeWindowMinutes, null, null, null);
    }

    public CreateWorkflowCommand(String projectId, String groupingStrategy, int timeWindowMinutes, String voiceProfileId) {
        this(projectId, groupingStrategy, timeWindowMinutes, voiceProfileId, null, null);
    }
}
