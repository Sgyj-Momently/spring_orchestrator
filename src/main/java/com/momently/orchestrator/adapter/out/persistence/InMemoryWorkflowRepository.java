package com.momently.orchestrator.adapter.out.persistence;

import com.momently.orchestrator.application.port.out.WorkflowRepository;
import com.momently.orchestrator.domain.Workflow;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * 로컬 개발과 테스트 실행을 위한 메모리 기반 워크플로 저장소다.
 *
 * <p>{@code memory} 프로필에서 Spring context를 가볍게 띄우기 위한 adapter다. 프로세스가 종료되면
 * 모든 데이터가 사라지므로 운영 저장소가 아니며, 상태 머신과 웹 API의 흐름을 빠르게 검증하는 용도에
 * 한정한다.</p>
 */
@Repository
@Profile("memory")
public class InMemoryWorkflowRepository implements WorkflowRepository {

    private final Map<UUID, Workflow> storage = new ConcurrentHashMap<>();

    /**
     * 워크플로 aggregate를 현재 JVM 메모리에 저장한다.
     *
     * <p>동일한 workflow id가 이미 있으면 최신 aggregate 참조로 교체한다. 영속 저장소가 아니므로
     * 재시작 후 복구나 트랜잭션 격리는 제공하지 않는다.</p>
     *
     * @param workflow 저장할 워크플로 aggregate
     * @return 저장된 aggregate
     */
    @Override
    public Workflow save(Workflow workflow) {
        storage.put(workflow.getWorkflowId(), workflow);
        return workflow;
    }

    /**
     * 현재 JVM 메모리에 저장된 워크플로를 식별자로 조회한다.
     *
     * @param workflowId 조회할 워크플로 식별자
     * @return 메모리에 존재하면 워크플로, 없으면 빈 Optional
     */
    @Override
    public Optional<Workflow> findById(UUID workflowId) {
        return Optional.ofNullable(storage.get(workflowId));
    }
}
