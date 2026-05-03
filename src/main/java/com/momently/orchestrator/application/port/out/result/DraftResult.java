package com.momently.orchestrator.application.port.out.result;

/**
 * 초안 작성 단계가 완료된 뒤 오케스트레이터에 남기는 최소 결과다.
 *
 * @param draftSectionCount 생성된 초안 섹션 수
 * @param resultPath 초안 JSON 아티팩트 파일 경로
 */
public record DraftResult(
    int draftSectionCount,
    String resultPath
) {
}

