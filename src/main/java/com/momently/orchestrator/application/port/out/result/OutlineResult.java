package com.momently.orchestrator.application.port.out.result;

/**
 * 개요 생성 단계가 완료된 뒤 오케스트레이터에 남기는 최소 결과다.
 *
 * @param outlineSectionCount 생성된 섹션 수(요약 지표)
 * @param resultPath 개요(JSON) 아티팩트 파일 경로
 */
public record OutlineResult(
    int outlineSectionCount,
    String resultPath
) {
}

