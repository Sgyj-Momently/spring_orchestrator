package com.momently.orchestrator.application.port.out.result;

/**
 * 민감정보 안전성 검사 단계가 완료된 뒤 오케스트레이터에 남기는 최소 결과다.
 *
 * @param publicPhotoCount 후속 단계에 공개 가능한 사진 수
 * @param excludedPhotoCount 공개 결과에서 제외한 사진 수
 * @param resultPath 민감정보 검토 결과 JSON 아티팩트 파일 경로
 * @param sanitizedBundlePath 공개 가능 사진만 남긴 bundle JSON 아티팩트 파일 경로
 */
public record PrivacySafetyResult(
    int publicPhotoCount,
    int excludedPhotoCount,
    String resultPath,
    String sanitizedBundlePath
) {
}

