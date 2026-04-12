package com.momently.orchestrator.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momently.orchestrator.application.port.out.PhotoGroupingAgentPort;
import com.momently.orchestrator.application.port.out.PhotoInfoAgentPort;
import com.momently.orchestrator.application.port.out.WorkflowRepository;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 워크플로 러너의 순차 실행 규칙을 검증한다.
 */
class WorkflowRunnerTest {

    @Test
    @DisplayName("러너는 사진 정보 추출 후 그룹화를 순서대로 실행한다")
    void runsPhotoInfoThenGroupingInOrder() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        List<String> executionLog = new ArrayList<>();
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            WorkflowStatus.CREATED
        );
        repository.save(workflow);
        WorkflowRunner workflowRunner = new WorkflowRunner(
            repository,
            new WorkflowStateMachine(),
            projectId -> {
                executionLog.add("photo-info:%s".formatted(projectId));
                return new PhotoInfoResult(10, "artifacts/photo-info/project-001/bundle.json");
            },
            payload -> {
                executionLog.add("photo-grouping:%s".formatted(payload.get("grouping_strategy")));
                return new PhotoGroupingResult("LOCATION_BASED", 3);
            }
        );

        Workflow updated = workflowRunner.runWorkflow(workflow.getWorkflowId());

        assertThat(executionLog).containsExactly(
            "photo-info:project-001",
            "photo-grouping:LOCATION_BASED"
        );
        assertThat(updated.getStatus()).isEqualTo(WorkflowStatus.PHOTO_GROUPED);
        assertThat(repository.findById(workflow.getWorkflowId()))
            .get()
            .extracting(Workflow::getStatus)
            .isEqualTo(WorkflowStatus.PHOTO_GROUPED);
    }

    @Test
    @DisplayName("중간 단계 실패 시 워크플로를 FAILED로 기록한다")
    void marksWorkflowAsFailedWhenAgentThrowsException() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            WorkflowStatus.CREATED
        );
        repository.save(workflow);
        WorkflowRunner workflowRunner = new WorkflowRunner(
            repository,
            new WorkflowStateMachine(),
            projectId -> new PhotoInfoResult(10, "artifacts/photo-info/project-001/bundle.json"),
            payload -> {
                throw new IllegalStateException("grouping agent timeout");
            }
        );

        assertThatThrownBy(() -> workflowRunner.runWorkflow(workflow.getWorkflowId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("grouping agent timeout");

        Workflow failedWorkflow = repository.findById(workflow.getWorkflowId()).orElseThrow();
        assertThat(failedWorkflow.getStatus()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(failedWorkflow.getLastFailedStep()).isEqualTo(WorkflowStatus.PHOTO_GROUPING.name());
        assertThat(failedWorkflow.getLastErrorMessage()).contains("grouping agent timeout");
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
    }
}
