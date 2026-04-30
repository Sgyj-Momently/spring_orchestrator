package com.momently.orchestrator.application.port.out.result;

/**
 * 사진 품질 점수화 단계가 완료된 뒤 오케스트레이터에 남기는 최소 결과다.
 *
 * @param scoredPhotoCount 점수가 부여된 사진 수
 * @param averageScore 평균 품질 점수
 * @param resultPath quality score JSON artifact 경로
 * @param scoredBundlePath 품질 점수를 포함한 bundle JSON artifact 경로
 */
public record QualityScoreResult(
    int scoredPhotoCount,
    double averageScore,
    String resultPath,
    String scoredBundlePath
) {
}

