package com.momently.orchestrator.application.port.out.result;

/**
 * 대표 사진 선택 단계가 완료된 뒤 오케스트레이터에 남기는 최소 결과다.
 *
 * <p>대표 사진 선택 결과는 후속 개요·초안 단계에서 사용될 수 있다. 오케스트레이터는
 * 대용량 본문 대신 아티팩트 파일 경로와 최소 카운터만 유지한다.</p>
 *
 * @param heroPhotoCount 선택된 대표 사진 수(보통 그룹 수와 같음)
 * @param resultPath 대표 사진 선택 결과 JSON 아티팩트 파일 경로
 */
public record HeroPhotoResult(
    int heroPhotoCount,
    String resultPath
) {

    /**
     * 아티팩트 경로가 아직 없는 대역 구현에서 쓰는 축약 생성자다.
     *
     * @param heroPhotoCount 선택된 대표 사진 수
     */
    public HeroPhotoResult(int heroPhotoCount) {
        this(heroPhotoCount, null);
    }
}

