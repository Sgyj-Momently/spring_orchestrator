package com.momently.orchestrator.application.port.out.result;

/**
 * 사진 정보 추출 단계가 완료된 뒤 오케스트레이터에 남기는 최소 결과다.
 *
 * <p>사진별 EXIF, 요약, bundle 본문은 크기가 커질 수 있으므로 애플리케이션 계층에 직접 싣지 않는다.
 * 이 결과는 상태 머신이 다음 단계로 넘어갈 수 있는지 판단하고, 그룹화 어댑터가 bundle 아티팩트를
 * 찾아 입력으로 변환할 수 있게 하는 참조 정보를 제공한다.</p>
 *
 * @param photoCount 파이프라인이 처리한 사진 수
 * @param bundlePath 그룹화 단계와 디버깅에서 참조할 bundle JSON 아티팩트 파일 경로
 * @param blogPath 파이프라인이 생성한 블로그 마크다운 아티팩트 경로, 없으면 null
 */
public record PhotoInfoResult(
    int photoCount,
    String bundlePath,
    String blogPath
) {

    /**
     * 블로그 생성 전 또는 대역 구현에서 bundle 경로만 반환할 때 쓰는 축약 생성자다.
     *
     * @param photoCount 파이프라인이 처리한 사진 수
     * @param bundlePath 그룹화 단계와 디버깅에서 참조할 bundle JSON 아티팩트 파일 경로
     */
    public PhotoInfoResult(int photoCount, String bundlePath) {
        this(photoCount, bundlePath, null);
    }
}
