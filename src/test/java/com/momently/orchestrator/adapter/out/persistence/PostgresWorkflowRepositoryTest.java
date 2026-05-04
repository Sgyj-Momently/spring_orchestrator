package com.momently.orchestrator.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * PostgreSQL 저장소 adapter가 Spring Data repository와 도메인 매핑을 올바르게 연결하는지 검증한다.
 */
class PostgresWorkflowRepositoryTest {

    private final JpaWorkflowSpringDataRepository springDataRepository = mock(JpaWorkflowSpringDataRepository.class);
    private final PostgresWorkflowRepository repository = new PostgresWorkflowRepository(springDataRepository);

    @Test
    @DisplayName("새 워크플로를 JPA entity로 변환해 저장하고 도메인으로 반환한다")
    void savesNewWorkflow() {
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            90,
            WorkflowStatus.CREATED
        );
        when(springDataRepository.findById(workflow.getWorkflowId())).thenReturn(Optional.empty());
        when(springDataRepository.save(any(WorkflowJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Workflow saved = repository.save(workflow);

        ArgumentCaptor<WorkflowJpaEntity> captor = ArgumentCaptor.forClass(WorkflowJpaEntity.class);
        verify(springDataRepository).save(captor.capture());
        assertThat(saved.getWorkflowId()).isEqualTo(workflow.getWorkflowId());
        assertThat(saved.getProjectId()).isEqualTo("project-001");
        assertThat(captor.getValue().toDomain().getGroupingStrategy()).isEqualTo("LOCATION_BASED");
    }

    @Test
    @DisplayName("기존 entity가 있으면 해당 entity를 업데이트해 저장한다")
    void updatesExistingWorkflowEntity() {
        UUID workflowId = UUID.randomUUID();
        Workflow existingWorkflow = new Workflow(
            workflowId,
            "old-project",
            "TIME_BASED",
            30,
            WorkflowStatus.CREATED
        );
        WorkflowJpaEntity existingEntity = WorkflowJpaEntity.fromDomain(existingWorkflow);
        Workflow updatedWorkflow = new Workflow(
            workflowId,
            "new-project",
            "SCENE_BASED",
            120,
            WorkflowStatus.PHOTO_INFO_EXTRACTED
        );
        updatedWorkflow.recordPhotoInfoArtifacts(3, "output/new-project/bundles/bundle.json", null);
        when(springDataRepository.findById(workflowId)).thenReturn(Optional.of(existingEntity));
        when(springDataRepository.save(any(WorkflowJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Workflow saved = repository.save(updatedWorkflow);

        assertThat(saved.getProjectId()).isEqualTo("new-project");
        assertThat(saved.getGroupingStrategy()).isEqualTo("SCENE_BASED");
        assertThat(saved.getPhotoCount()).isEqualTo(3);
        verify(springDataRepository).save(existingEntity);
    }

    @Test
    @DisplayName("식별자 조회 결과를 도메인 Optional로 변환한다")
    void findsWorkflowById() {
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            90,
            WorkflowStatus.CREATED
        );
        when(springDataRepository.findById(workflow.getWorkflowId()))
            .thenReturn(Optional.of(WorkflowJpaEntity.fromDomain(workflow)));

        Optional<Workflow> found = repository.findById(workflow.getWorkflowId());

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getProjectId()).isEqualTo("project-001");
    }

    @Test
    @DisplayName("목록 조회는 최신 수정 순 Spring Data 메서드 결과를 도메인 목록으로 변환한다")
    void findsAllWorkflows() {
        Workflow first = new Workflow(UUID.randomUUID(), "project-001", "LOCATION_BASED", 90, WorkflowStatus.CREATED);
        Workflow second = new Workflow(UUID.randomUUID(), "project-002", "TIME_BASED", 45, WorkflowStatus.CREATED);
        when(springDataRepository.findAllByOrderByUpdatedAtDesc())
            .thenReturn(List.of(WorkflowJpaEntity.fromDomain(first), WorkflowJpaEntity.fromDomain(second)));

        List<Workflow> workflows = repository.findAll();

        assertThat(workflows).extracting(Workflow::getProjectId).containsExactly("project-001", "project-002");
    }

    @Test
    @DisplayName("전체 삭제는 batch delete로 위임한다")
    void deletesAllWorkflows() {
        repository.deleteAll();

        verify(springDataRepository).deleteAllInBatch();
    }
}
