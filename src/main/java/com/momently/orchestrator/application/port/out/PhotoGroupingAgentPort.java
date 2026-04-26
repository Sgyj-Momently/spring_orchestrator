package com.momently.orchestrator.application.port.out;

import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;

/**
 * 사진 그룹화 단계를 application 계층에서 호출하기 위한 outbound 포트다.
 *
 * <p>이 포트는 Spring 오케스트레이터가 FastAPI 그룹화 에이전트의 HTTP 주소, 응답 DTO,
 * 클라이언트 구현 방식을 직접 알지 못하게 하는 경계다. 구현체는 사진 정보 추출 단계의 artifact를
 * 그룹화 에이전트가 이해하는 계약으로 변환하고, 오케스트레이션에 필요한 최소 결과만
 * {@link PhotoGroupingResult}로 변환해야 한다.</p>
 */
public interface PhotoGroupingAgentPort {

    /**
     * 사진 정보 추출 결과를 외부 그룹화 에이전트 또는 대역 구현체에 전달한다.
     *
     * <p>application 계층은 bundle JSON의 내부 구조나 FastAPI 요청 필드를 알지 않는다. 구현체는
     * {@link PhotoInfoResult#bundlePath()}가 가리키는 artifact를 읽어 사진 목록을 포함한 요청으로
     * 변환하고, 외부 호출 실패를 application 계층에서 처리 가능한 런타임 예외로 변환해야 한다.</p>
     *
     * @param projectId 워크플로에 연결된 프로젝트 식별자
     * @param groupingStrategy 워크플로 생성 시 선택된 그룹화 전략
     * @param timeWindowMinutes 같은 그룹으로 묶을 최대 시간 간격(분)
     * @param photoInfoResult 사진 정보 추출 단계가 남긴 bundle artifact 참조
     * @return 실제 적용된 전략과 생성된 그룹 수를 담은 요약 결과
     */
    PhotoGroupingResult groupPhotos(
        String projectId,
        String groupingStrategy,
        int timeWindowMinutes,
        PhotoInfoResult photoInfoResult
    );
}
