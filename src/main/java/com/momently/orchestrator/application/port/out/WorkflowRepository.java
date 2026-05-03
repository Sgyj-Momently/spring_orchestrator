package com.momently.orchestrator.application.port.out;

import com.momently.orchestrator.domain.Workflow;
import java.util.Optional;
import java.util.UUID;

/**
 * 워크플로 집합체를 저장·조회하기 위한 애플리케이션 계층 영속 포트다.
 *
 * <p>도메인과 애플리케이션 서비스는 JPA·메모리·PostgreSQL 같은 저장 기술을 직접 알지 않는다.
 * 모든 저장 구현체는 이 포트를 통해 {@link Workflow} 집합체 단위로 상태를 보존해야 하며,
 * 상태 머신이 허용한 전이가 저장소를 거치며 손실되지 않도록 한다.</p>
 */
public interface WorkflowRepository {

    /**
     * 워크플로 집합체의 현재 상태와 실패 메타데이터를 저장한다.
     *
     * @param workflow 저장할 워크플로 집합체
     * @return 저장소에 반영된 워크플로 집합체
     */
    Workflow save(Workflow workflow);

    /**
     * 공개 워크플로 식별자로 저장된 집합체를 조회한다.
     *
     * @param workflowId API와 HATEOAS 링크에서 사용되는 워크플로 식별자
     * @return 조회된 워크플로, 존재하지 않으면 빈 Optional
     */
    Optional<Workflow> findById(UUID workflowId);
}
