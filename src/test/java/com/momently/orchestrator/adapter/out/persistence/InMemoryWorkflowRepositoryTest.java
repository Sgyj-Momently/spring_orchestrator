package com.momently.orchestrator.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 메모리 워크플로 저장소의 저장/조회 동작을 검증한다.
 */
class InMemoryWorkflowRepositoryTest {

    @Test
    @DisplayName("워크플로를 저장하고 식별자로 다시 조회한다")
    void savesAndFindsWorkflow() {
        InMemoryWorkflowRepository repository = new InMemoryWorkflowRepository();
        UUID workflowId = UUID.randomUUID();
        Workflow workflow = new Workflow(
            workflowId,
            "project-001",
            "LOCATION_BASED",
            WorkflowStatus.CREATED
        );

        Workflow saved = repository.save(workflow);

        assertThat(saved).isSameAs(workflow);
        assertThat(repository.findById(workflowId)).containsSame(workflow);
    }

    @Test
    @DisplayName("없는 워크플로 식별자는 빈 Optional을 반환한다")
    void returnsEmptyWhenWorkflowDoesNotExist() {
        InMemoryWorkflowRepository repository = new InMemoryWorkflowRepository();

        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }
}
