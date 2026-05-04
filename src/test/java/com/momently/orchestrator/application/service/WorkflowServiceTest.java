package com.momently.orchestrator.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momently.orchestrator.application.port.in.command.CreateWorkflowCommand;
import com.momently.orchestrator.application.port.out.WorkflowRepository;
import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 워크플로 서비스의 상태 관리 책임을 검증한다.
 */
class WorkflowServiceTest {

    @Test
    @DisplayName("워크플로 생성 시 초기 상태는 CREATED다")
    void createsWorkflowWithCreatedStatus() {
        WorkflowService workflowService = new WorkflowService(
            new InMemoryWorkflowRepositoryStub(),
            new WorkflowStateMachine()
        );

        Workflow workflow = workflowService.createWorkflow(
            new CreateWorkflowCommand("project-001", "LOCATION_BASED", 90)
        );

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.CREATED);
    }

    @Test
    @DisplayName("서비스는 상태 머신 규칙에 맞는 전이만 저장한다")
    void advancesWorkflowWhenTransitionIsAllowed() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            90,
            WorkflowStatus.CREATED
        );
        repository.save(workflow);
        WorkflowService workflowService = new WorkflowService(repository, new WorkflowStateMachine());

        Workflow updated = workflowService.advanceWorkflow(
            workflow.getWorkflowId(),
            WorkflowStatus.PHOTO_INFO_EXTRACTING
        );

        assertThat(updated.getStatus()).isEqualTo(WorkflowStatus.PHOTO_INFO_EXTRACTING);
        assertThat(repository.findById(workflow.getWorkflowId()))
            .get()
            .extracting(Workflow::getStatus)
            .isEqualTo(WorkflowStatus.PHOTO_INFO_EXTRACTING);
    }

    @Test
    @DisplayName("서비스는 잘못된 상태 전이를 차단한다")
    void rejectsInvalidTransition() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            90,
            WorkflowStatus.CREATED
        );
        repository.save(workflow);
        WorkflowService workflowService = new WorkflowService(repository, new WorkflowStateMachine());

        assertThatThrownBy(() -> workflowService.advanceWorkflow(
            workflow.getWorkflowId(),
            WorkflowStatus.PHOTO_GROUPING
        ))
            .isInstanceOf(IllegalStateException.class);
    }

    /**
     * 테스트 전용 메모리 저장소 구현체다.
     */
    private static final class InMemoryWorkflowRepositoryStub implements WorkflowRepository {

        private final Map<UUID, Workflow> storage = new HashMap<>();

        @Override
        public Workflow save(Workflow workflow) {
            storage.put(workflow.getWorkflowId(), workflow);
            return workflow;
        }

        @Override
        public Optional<Workflow> findById(UUID workflowId) {
            return Optional.ofNullable(storage.get(workflowId));
        }

        @Override
        public List<Workflow> findAll() {
            return List.copyOf(storage.values());
        }

        @Override
        public void deleteAll() {
            storage.clear();
        }
    }
}
