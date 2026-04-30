package com.momently.orchestrator.application.port.out.result;

/**
 * 최종 검수 단계가 완료된 뒤 오케스트레이터에 남기는 최소 결과다.
 *
 * @param issueCount 검수에서 발견한 이슈 수
 * @param resultPath review JSON artifact 경로
 */
public record ReviewResult(
    int issueCount,
    String resultPath
) {
}

