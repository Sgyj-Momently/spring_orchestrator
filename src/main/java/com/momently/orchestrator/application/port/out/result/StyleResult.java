package com.momently.orchestrator.application.port.out.result;

/**
 * 문체 적용 단계가 완료된 뒤 오케스트레이터에 남기는 최소 결과다.
 *
 * @param wordCount 스타일 적용 후 단어 수
 * @param resultPath 문체 적용 결과 JSON 아티팩트 파일 경로
 */
public record StyleResult(
    int wordCount,
    String resultPath
) {
}

