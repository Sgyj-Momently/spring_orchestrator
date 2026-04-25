package com.momently.orchestrator.application.port.out;

import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import java.util.Map;

/**
 * 사진 그룹화 단계를 application 계층에서 호출하기 위한 outbound 포트다.
 *
 * <p>이 포트는 Spring 오케스트레이터가 FastAPI 그룹화 에이전트의 HTTP 주소, 응답 DTO,
 * 클라이언트 구현 방식을 직접 알지 못하게 하는 경계다. 구현체는 전달받은 payload를
 * 그룹화 에이전트가 이해하는 계약으로 전송하고, 오케스트레이션에 필요한 최소 결과만
 * {@link PhotoGroupingResult}로 변환해야 한다.</p>
 */
public interface PhotoGroupingAgentPort {

    /**
     * 사진 그룹화 요청 payload를 외부 에이전트 또는 대역 구현체에 전달한다.
     *
     * <p>payload에는 최소한 {@code project_id}, {@code grouping_strategy}가 포함되어야 하며,
     * 후속 구현에서는 사진 목록이나 bundle 변환 결과가 함께 들어갈 수 있다. 구현체는 외부 호출 실패를
     * application 계층에서 처리 가능한 런타임 예외로 변환해야 한다.</p>
     *
     * @param payload 그룹화 에이전트 공개 계약에 맞춘 요청 본문
     * @return 실제 적용된 전략과 생성된 그룹 수를 담은 요약 결과
     */
    PhotoGroupingResult groupPhotos(Map<String, Object> payload);
}
