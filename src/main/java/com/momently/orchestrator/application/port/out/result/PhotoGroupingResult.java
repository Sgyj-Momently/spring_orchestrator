package com.momently.orchestrator.application.port.out.result;

/**
 * 사진 그룹화 단계가 완료된 뒤 오케스트레이터에 남기는 최소 결과다.
 *
 * <p>전체 그룹 JSON은 후속 단계에서 artifact로 저장하거나 전달할 수 있지만,
 * 상태 전이와 로그에는 우선 이 요약 값만 필요하다. 포트 결과를 작게 유지하면
 * application 계층이 FastAPI 응답 구조 전체에 결합되지 않는다.</p>
 *
 * @param groupingStrategy 그룹화 에이전트가 실제 적용한 enum 전략 값
 * @param groupCount 후속 대표 사진 선택 단계가 예상할 수 있는 생성 그룹 수
 * @param resultPath FastAPI 그룹화 응답 전체를 저장한 JSON artifact 경로
 */
public record PhotoGroupingResult(
    String groupingStrategy,
    int groupCount,
    String resultPath
) {

    /**
     * 그룹화 응답 artifact가 아직 없는 대역 구현에서 쓰는 축약 생성자다.
     *
     * @param groupingStrategy 그룹화 에이전트가 실제 적용한 enum 전략 값
     * @param groupCount 후속 대표 사진 선택 단계가 예상할 수 있는 생성 그룹 수
     */
    public PhotoGroupingResult(String groupingStrategy, int groupCount) {
        this(groupingStrategy, groupCount, null);
    }
}
