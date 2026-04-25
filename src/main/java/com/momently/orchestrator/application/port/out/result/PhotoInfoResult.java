package com.momently.orchestrator.application.port.out.result;

/**
 * 사진 정보 추출 단계가 완료된 뒤 오케스트레이터에 남기는 최소 결과다.
 *
 * <p>사진별 EXIF, 요약, bundle 본문은 크기가 커질 수 있으므로 application 계층에 직접 싣지 않는다.
 * 이 결과는 상태 머신이 다음 단계로 넘어갈 수 있는지 판단하고, 그룹화 adapter가 bundle artifact를
 * 찾아 입력으로 변환할 수 있게 하는 참조 정보를 제공한다.</p>
 *
 * @param photoCount 파이프라인이 처리한 사진 수
 * @param bundlePath 그룹화 단계와 디버깅에서 참조할 bundle JSON artifact 경로
 */
public record PhotoInfoResult(
    int photoCount,
    String bundlePath
) {
}
