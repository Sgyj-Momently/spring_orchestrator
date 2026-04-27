package com.momently.orchestrator.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momently.orchestrator.application.port.out.HeroPhotoAgentPort;
import com.momently.orchestrator.application.port.out.PhotoGroupingAgentPort;
import com.momently.orchestrator.application.port.out.PhotoInfoAgentPort;
import com.momently.orchestrator.application.port.out.WorkflowRepository;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
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
    @DisplayName("러너는 사진 정보 추출 후 그룹화, 대표 사진 선택, 개요 생성을 순서대로 실행한다")
    void runsPhotoInfoThenGroupingThenHeroPhotoThenOutlineInOrder() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        List<String> executionLog = new ArrayList<>();
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            90,
            WorkflowStatus.CREATED
        );
        repository.save(workflow);
        WorkflowRunner workflowRunner = new WorkflowRunner(
            repository,
            new WorkflowStateMachine(),
            projectId -> {
                executionLog.add("photo-info:%s".formatted(projectId));
                return new PhotoInfoResult(
                    10,
                    "artifacts/photo-info/project-001/bundle.json",
                    "artifacts/photo-info/project-001/blog.md"
                );
            },
            (projectId, groupingStrategy, timeWindowMinutes, photoInfoResult) -> {
                executionLog.add("photo-grouping:%s:%s".formatted(groupingStrategy, photoInfoResult.bundlePath()));
                return new PhotoGroupingResult(
                    "LOCATION_BASED",
                    3,
                    "artifacts/photo-grouping/project-001/grouping-result.json"
                );
            },
            (projectId, photoInfoResult, photoGroupingResult) -> {
                executionLog.add(
                    "hero-photo:%s:%s"
                        .formatted(photoInfoResult.bundlePath(), photoGroupingResult.resultPath())
                );
                return new HeroPhotoResult(
                    photoGroupingResult.groupCount(),
                    "artifacts/hero-photo/project-001/hero-result.json"
                );
            },
            (projectId, photoInfoResult, photoGroupingResult, heroPhotoResult) -> {
                executionLog.add("outline:%s".formatted(heroPhotoResult.resultPath()));
                return new OutlineResult(4, "artifacts/outline/project-001/outline.json");
            }
        );

        workflowRunner.runWorkflow(workflow.getWorkflowId());
        Workflow updated = repository.findById(workflow.getWorkflowId()).orElseThrow();

        assertThat(executionLog).containsExactly(
            "photo-info:project-001",
            "photo-grouping:LOCATION_BASED:artifacts/photo-info/project-001/bundle.json",
            "hero-photo:artifacts/photo-info/project-001/bundle.json:artifacts/photo-grouping/project-001/grouping-result.json",
            "outline:artifacts/hero-photo/project-001/hero-result.json"
        );
        assertThat(updated.getStatus()).isEqualTo(WorkflowStatus.OUTLINE_CREATED);
        assertThat(updated.getPhotoCount()).isEqualTo(10);
        assertThat(updated.getGroupCount()).isEqualTo(3);
        assertThat(updated.getHeroPhotoCount()).isEqualTo(3);
        assertThat(updated.getOutlineSectionCount()).isEqualTo(4);
        assertThat(updated.getPhotoInfoBundlePath()).isEqualTo("artifacts/photo-info/project-001/bundle.json");
        assertThat(updated.getBlogPath()).isEqualTo("artifacts/photo-info/project-001/blog.md");
        assertThat(updated.getGroupingResultPath())
            .isEqualTo("artifacts/photo-grouping/project-001/grouping-result.json");
        assertThat(updated.getHeroPhotoResultPath())
            .isEqualTo("artifacts/hero-photo/project-001/hero-result.json");
        assertThat(updated.getOutlineResultPath())
            .isEqualTo("artifacts/outline/project-001/outline.json");
        assertThat(repository.findById(workflow.getWorkflowId()))
            .get()
            .extracting(Workflow::getStatus)
            .isEqualTo(WorkflowStatus.OUTLINE_CREATED);
    }

    @Test
    @DisplayName("중간 단계 실패 시 워크플로를 FAILED로 기록한다")
    void marksWorkflowAsFailedWhenAgentThrowsException() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            90,
            WorkflowStatus.CREATED
        );
        repository.save(workflow);
        WorkflowRunner workflowRunner = new WorkflowRunner(
            repository,
            new WorkflowStateMachine(),
            projectId -> new PhotoInfoResult(10, "artifacts/photo-info/project-001/bundle.json"),
            (projectId, groupingStrategy, timeWindowMinutes, photoInfoResult) -> {
                throw new IllegalStateException("grouping agent timeout");
            },
            (projectId, photoInfoResult, photoGroupingResult) -> new HeroPhotoResult(0, null),
            (projectId, photoInfoResult, photoGroupingResult, heroPhotoResult) -> new OutlineResult(0, null)
        );

        assertThatThrownBy(() -> workflowRunner.runWorkflow(workflow.getWorkflowId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("grouping agent timeout");

        Workflow failedWorkflow = repository.findById(workflow.getWorkflowId()).orElseThrow();
        assertThat(failedWorkflow.getStatus()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(failedWorkflow.getLastFailedStep()).isEqualTo(WorkflowStatus.PHOTO_GROUPING.name());
        assertThat(failedWorkflow.getLastErrorMessage()).contains("grouping agent timeout");
    }

    @Test
    @DisplayName("이미 outline artifact가 있으면 재실행 시 아무 에이전트도 호출하지 않는다")
    void skipsAllStepsWhenOutlineAlreadyExists() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            90,
            WorkflowStatus.OUTLINE_CREATED
        );
        workflow.recordOutlineArtifacts(4, "artifacts/outline/project-001/outline.json");
        repository.save(workflow);
        List<String> executionLog = new ArrayList<>();
        WorkflowRunner workflowRunner = new WorkflowRunner(
            repository,
            new WorkflowStateMachine(),
            projectId -> {
                executionLog.add("photo-info");
                return new PhotoInfoResult(0, null);
            },
            (projectId, groupingStrategy, timeWindowMinutes, photoInfoResult) -> {
                executionLog.add("grouping");
                return new PhotoGroupingResult("LOCATION_BASED", 0, null);
            },
            (projectId, photoInfoResult, photoGroupingResult) -> {
                executionLog.add("hero");
                return new HeroPhotoResult(0, null);
            },
            (projectId, photoInfoResult, photoGroupingResult, heroPhotoResult) -> {
                executionLog.add("outline");
                return new OutlineResult(0, null);
            }
        );

        workflowRunner.runWorkflow(workflow.getWorkflowId());

        assertThat(executionLog).isEmpty();
        assertThat(repository.findById(workflow.getWorkflowId())).get()
            .extracting(Workflow::getStatus)
            .isEqualTo(WorkflowStatus.OUTLINE_CREATED);
    }

    @Test
    @DisplayName("FAILED 재실행 시 grouping artifact가 있으면 대표 사진 단계부터 재개한다")
    void resumesFromHeroPhotoWhenGroupingArtifactExists() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            90,
            WorkflowStatus.FAILED
        );
        workflow.recordPhotoInfoArtifacts(2, "artifacts/photo-info/project-001/bundle.json", null);
        workflow.recordGroupingArtifacts(1, "artifacts/photo-grouping/project-001/grouping-result.json");
        repository.save(workflow);
        List<String> executionLog = new ArrayList<>();
        WorkflowRunner workflowRunner = new WorkflowRunner(
            repository,
            new WorkflowStateMachine(),
            projectId -> {
                executionLog.add("photo-info");
                return new PhotoInfoResult(0, null);
            },
            (projectId, groupingStrategy, timeWindowMinutes, photoInfoResult) -> {
                executionLog.add("grouping");
                return new PhotoGroupingResult("LOCATION_BASED", 0, null);
            },
            (projectId, photoInfoResult, photoGroupingResult) -> {
                executionLog.add("hero:" + photoGroupingResult.resultPath());
                return new HeroPhotoResult(1, "artifacts/hero-photo/project-001/hero-result.json");
            },
            (projectId, photoInfoResult, photoGroupingResult, heroPhotoResult) -> {
                executionLog.add("outline:" + heroPhotoResult.resultPath());
                return new OutlineResult(3, "artifacts/outline/project-001/outline.json");
            }
        );

        workflowRunner.runWorkflow(workflow.getWorkflowId());

        assertThat(executionLog).containsExactly(
            "hero:artifacts/photo-grouping/project-001/grouping-result.json",
            "outline:artifacts/hero-photo/project-001/hero-result.json"
        );
        assertThat(repository.findById(workflow.getWorkflowId())).get()
            .extracting(Workflow::getStatus)
            .isEqualTo(WorkflowStatus.OUTLINE_CREATED);
    }

    @Test
    @DisplayName("FAILED 재실행 시 photo info artifact만 있으면 그룹화 단계부터 재개한다")
    void resumesFromGroupingWhenPhotoInfoArtifactExists() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            90,
            WorkflowStatus.FAILED
        );
        workflow.recordPhotoInfoArtifacts(2, "artifacts/photo-info/project-001/bundle.json", null);
        repository.save(workflow);
        List<String> executionLog = new ArrayList<>();
        WorkflowRunner workflowRunner = new WorkflowRunner(
            repository,
            new WorkflowStateMachine(),
            projectId -> {
                executionLog.add("photo-info");
                return new PhotoInfoResult(0, null);
            },
            (projectId, groupingStrategy, timeWindowMinutes, photoInfoResult) -> {
                executionLog.add("grouping:" + photoInfoResult.bundlePath());
                return new PhotoGroupingResult(
                    "LOCATION_BASED",
                    1,
                    "artifacts/photo-grouping/project-001/grouping-result.json"
                );
            },
            (projectId, photoInfoResult, photoGroupingResult) -> {
                executionLog.add("hero");
                return new HeroPhotoResult(1, "artifacts/hero-photo/project-001/hero-result.json");
            },
            (projectId, photoInfoResult, photoGroupingResult, heroPhotoResult) -> {
                executionLog.add("outline");
                return new OutlineResult(3, "artifacts/outline/project-001/outline.json");
            }
        );

        workflowRunner.runWorkflow(workflow.getWorkflowId());

        assertThat(executionLog).containsExactly(
            "grouping:artifacts/photo-info/project-001/bundle.json",
            "hero",
            "outline"
        );
    }

    @Test
    @DisplayName("실행 중인 워크플로는 중복 실행을 거부한다")
    void rejectsAlreadyRunningWorkflow() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            90,
            WorkflowStatus.PHOTO_GROUPING
        );
        repository.save(workflow);
        WorkflowRunner workflowRunner = new WorkflowRunner(
            repository,
            new WorkflowStateMachine(),
            projectId -> new PhotoInfoResult(0, null),
            (projectId, groupingStrategy, timeWindowMinutes, photoInfoResult) -> new PhotoGroupingResult("LOCATION_BASED", 0, null),
            (projectId, photoInfoResult, photoGroupingResult) -> new HeroPhotoResult(0, null),
            (projectId, photoInfoResult, photoGroupingResult, heroPhotoResult) -> new OutlineResult(0, null)
        );

        assertThatThrownBy(() -> workflowRunner.runWorkflow(workflow.getWorkflowId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Workflow is already running");
    }

    @Test
    @DisplayName("존재하지 않는 워크플로 실행 요청은 실패한다")
    void failsWhenWorkflowIsMissing() {
        WorkflowRunner workflowRunner = new WorkflowRunner(
            new InMemoryWorkflowRepositoryStub(),
            new WorkflowStateMachine(),
            projectId -> new PhotoInfoResult(0, null),
            (projectId, groupingStrategy, timeWindowMinutes, photoInfoResult) -> new PhotoGroupingResult("LOCATION_BASED", 0, null),
            (projectId, photoInfoResult, photoGroupingResult) -> new HeroPhotoResult(0, null),
            (projectId, photoInfoResult, photoGroupingResult, heroPhotoResult) -> new OutlineResult(0, null)
        );

        assertThatThrownBy(() -> workflowRunner.runWorkflow(UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Workflow not found");
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
