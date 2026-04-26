package com.momently.orchestrator.application.port.out;

import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;

/**
 * 사진 정보 추출 단계를 application 계층에서 호출하기 위한 outbound 포트다.
 *
 * <p>현재 사진 정보 추출 구현은 CLI 파이프라인이고, 이후 FastAPI 서비스로 바뀔 수 있다.
 * 이 포트는 {@code WorkflowRunner}가 그런 실행 방식의 차이를 알지 못하게 하며,
 * 후속 그룹화 단계가 사용할 bundle artifact 위치와 사진 수만 안정적으로 제공한다.</p>
 */
public interface PhotoInfoAgentPort {

    /**
     * 프로젝트 식별자에 해당하는 사진 입력을 분석하고 bundle artifact를 생성한다.
     *
     * <p>구현체는 프로젝트 식별자를 내부 입력 위치나 원격 요청으로 변환한다. 공개 API 요청에는
     * 파일 경로, 모델명, Ollama 주소 같은 인프라 값을 노출하지 않는다는 원칙을 지켜야 한다.</p>
     *
     * @param projectId 워크플로 생성 시 받은 프로젝트 식별자
     * @return 추출된 사진 수와 생성된 bundle artifact 경로
     */
    PhotoInfoResult extractPhotoInfo(String projectId);
}
