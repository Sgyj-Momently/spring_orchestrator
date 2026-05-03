package com.momently.orchestrator.application.port.out.result;

/**
 * 사진 품질 점수화 단계가 완료된 뒤 오케스트레이터에 남기는 최소 결과다.
 *
 * @param scoredPhotoCount 점수가 부여된 사진 수
 * @param averageScore 평균 품질 점수
 * @param resultPath 품질 점수 결과 JSON 아티팩트 파일 경로
 * @param scoredBundlePath 점수가 반영된 bundle JSON 아티팩트 파일 경로
 */
public record QualityScoreResult(
    int scoredPhotoCount,
    double averageScore,
    String resultPath,
    String scoredBundlePath
) {
}

