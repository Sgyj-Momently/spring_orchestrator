package com.momently.orchestrator.application.port.in.command;

/**
 * 워크플로 생성에 필요한 입력을 담는 애플리케이션 command다.
 *
 * @param projectId 프로젝트 식별자
 * @param groupingStrategy 그룹화 전략
 * @param timeWindowMinutes 같은 그룹으로 묶을 최대 시간 간격(분)
 */
public record CreateWorkflowCommand(
    String projectId,
    String groupingStrategy,
    int timeWindowMinutes
) {
}
