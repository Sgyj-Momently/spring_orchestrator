package com.momently.orchestrator.application.port.out;

import com.momently.orchestrator.domain.Workflow;

/**
 * 워크플로 상태 변경을 외부 구독자에게 알리는 출력 포트다.
 */
public interface WorkflowEventPort {

    /**
     * 워크플로의 현재 상태를 이벤트로 발행한다.
     *
     * @param workflow 발행할 워크플로 상태
     */
    void publish(Workflow workflow);
}
