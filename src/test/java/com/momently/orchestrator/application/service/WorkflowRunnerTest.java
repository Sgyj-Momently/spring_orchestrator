package com.momently.orchestrator.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momently.orchestrator.application.port.out.WorkflowRepository;
import com.momently.orchestrator.application.port.out.result.DraftResult;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.PrivacySafetyResult;
import com.momently.orchestrator.application.port.out.result.QualityScoreResult;
import com.momently.orchestrator.application.port.out.result.ReviewResult;
import com.momently.orchestrator.application.port.out.result.StyleResult;
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
    @DisplayName("러너는 사진 정보부터 최종 검수까지 순서대로 실행한다")
    void runsFullWorkflowInOrder() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        List<String> executionLog = new ArrayList<>();
        Workflow workflow = new Workflow(UUID.randomUUID(), "project-001", "LOCATION_BASED", 90, WorkflowStatus.CREATED);
        repository.save(workflow);
        WorkflowRunner runner = runner(repository, executionLog);

        runner.runWorkflow(workflow.getWorkflowId());

        Workflow updated = repository.findById(workflow.getWorkflowId()).orElseThrow();
        assertThat(executionLog).containsExactly(
            "photo-info:project-001",
            "privacy:artifacts/photo-info/project-001/bundle.json",
            "quality:artifacts/privacy/project-001/bundle.json",
            "photo-grouping:LOCATION_BASED:artifacts/quality/project-001/bundle.json",
            "hero-photo:artifacts/photo-grouping/project-001/grouping-result.json",
            "outline:artifacts/hero-photo/project-001/hero-result.json",
            "draft:artifacts/outline/project-001/outline.json",
            "style:artifacts/draft/project-001/draft.json",
            "review:artifacts/style/project-001/styled.json"
        );
        assertThat(updated.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(updated.getPhotoCount()).isEqualTo(10);
        assertThat(updated.getAverageQualityScore()).isEqualTo(0.75);
        assertThat(updated.getGroupCount()).isEqualTo(3);
        assertThat(updated.getHeroPhotoCount()).isEqualTo(3);
        assertThat(updated.getOutlineSectionCount()).isEqualTo(4);
        assertThat(updated.getDraftSectionCount()).isEqualTo(4);
        assertThat(updated.getStyledWordCount()).isEqualTo(150);
        assertThat(updated.getReviewIssueCount()).isZero();
        assertThat(updated.getReviewResultPath()).isEqualTo("artifacts/review/project-001/final.json");
    }

    @Test
    @DisplayName("중간 단계 실패 시 워크플로를 FAILED로 기록한다")
    void marksWorkflowAsFailedWhenAgentThrowsException() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(UUID.randomUUID(), "project-001", "LOCATION_BASED", 90, WorkflowStatus.CREATED);
        repository.save(workflow);
        WorkflowRunner runner = new WorkflowRunner(
            repository,
            new WorkflowStateMachine(),
            projectId -> new PhotoInfoResult(10, "artifacts/photo-info/project-001/bundle.json"),
            (projectId, photoInfoResult) -> new PrivacySafetyResult(
                10,
                0,
                "artifacts/privacy/project-001/privacy-result.json",
                "artifacts/privacy/project-001/bundle.json"
            ),
            (projectId, photoInfoResult) -> new QualityScoreResult(
                10,
                0.75,
                "artifacts/quality/project-001/quality-result.json",
                "artifacts/quality/project-001/bundle.json"
            ),
            (projectId, groupingStrategy, timeWindowMinutes, photoInfoResult) -> {
                throw new IllegalStateException("grouping agent timeout");
            },
            (projectId, photoInfoResult, photoGroupingResult) -> new HeroPhotoResult(0, null),
            (projectId, photoInfoResult, photoGroupingResult, heroPhotoResult) -> new OutlineResult(0, null),
            (projectId, photoInfoResult, photoGroupingResult, heroPhotoResult, outlineResult) -> new DraftResult(0, null),
            (projectId, draftResult, voiceProfileId) -> new StyleResult(0, null),
            (projectId, photoInfoResult, styleResult) -> new ReviewResult(0, null)
        );

        assertThatThrownBy(() -> runner.runWorkflow(workflow.getWorkflowId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("grouping agent timeout");

        Workflow failedWorkflow = repository.findById(workflow.getWorkflowId()).orElseThrow();
        assertThat(failedWorkflow.getStatus()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(failedWorkflow.getLastFailedStep()).isEqualTo(WorkflowStatus.PHOTO_GROUPING.name());
        assertThat(failedWorkflow.getLastErrorMessage()).contains("grouping agent timeout");
    }

    @Test
    @DisplayName("최종 artifact가 있으면 재실행 시 아무 에이전트도 호출하지 않는다")
    void skipsAllStepsWhenFinalReviewAlreadyExists() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(UUID.randomUUID(), "project-001", "LOCATION_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordReviewArtifacts(0, "artifacts/review/project-001/final.json");
        repository.save(workflow);
        List<String> executionLog = new ArrayList<>();
        WorkflowRunner runner = runner(repository, executionLog);

        runner.runWorkflow(workflow.getWorkflowId());

        assertThat(executionLog).isEmpty();
        assertThat(repository.findById(workflow.getWorkflowId())).get()
            .extracting(Workflow::getStatus)
            .isEqualTo(WorkflowStatus.COMPLETED);
    }

    @Test
    @DisplayName("FAILED 재실행 시 outline artifact가 있으면 초안 단계부터 재개한다")
    void resumesFromDraftWhenOutlineArtifactExists() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(UUID.randomUUID(), "project-001", "LOCATION_BASED", 90, WorkflowStatus.FAILED);
        workflow.recordPhotoInfoArtifacts(2, "artifacts/photo-info/project-001/bundle.json", null);
        workflow.recordGroupingArtifacts(1, "artifacts/photo-grouping/project-001/grouping-result.json");
        workflow.recordHeroPhotoArtifacts(1, "artifacts/hero-photo/project-001/hero-result.json");
        workflow.recordOutlineArtifacts(2, "artifacts/outline/project-001/outline.json");
        repository.save(workflow);
        List<String> executionLog = new ArrayList<>();
        WorkflowRunner runner = runner(repository, executionLog);

        runner.runWorkflow(workflow.getWorkflowId());

        assertThat(executionLog).containsExactly(
            "draft:artifacts/outline/project-001/outline.json",
            "style:artifacts/draft/project-001/draft.json",
            "review:artifacts/style/project-001/styled.json"
        );
        assertThat(repository.findById(workflow.getWorkflowId())).get()
            .extracting(Workflow::getStatus)
            .isEqualTo(WorkflowStatus.COMPLETED);
    }

    @Test
    @DisplayName("FAILED 재실행 시 draft artifact가 있으면 style 단계부터 재개한다")
    void resumesFromStyleWhenDraftArtifactExists() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(UUID.randomUUID(), "project-001", "LOCATION_BASED", 90, WorkflowStatus.FAILED);
        workflow.recordPhotoInfoArtifacts(2, "artifacts/photo-info/project-001/bundle.json", null);
        workflow.recordDraftArtifacts(2, "artifacts/draft/project-001/draft.json");
        repository.save(workflow);
        List<String> executionLog = new ArrayList<>();
        WorkflowRunner runner = runner(repository, executionLog);

        runner.runWorkflow(workflow.getWorkflowId());

        assertThat(executionLog).containsExactly(
            "style:artifacts/draft/project-001/draft.json",
            "review:artifacts/style/project-001/styled.json"
        );
        assertThat(repository.findById(workflow.getWorkflowId())).get()
            .extracting(Workflow::getStatus)
            .isEqualTo(WorkflowStatus.COMPLETED);
    }

    @Test
    @DisplayName("FAILED 재실행 시 style artifact가 있으면 review 단계부터 재개한다")
    void resumesFromReviewWhenStyleArtifactExists() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(UUID.randomUUID(), "project-001", "LOCATION_BASED", 90, WorkflowStatus.FAILED);
        workflow.recordPhotoInfoArtifacts(2, "artifacts/photo-info/project-001/bundle.json", null);
        workflow.recordStyleArtifacts(140, "artifacts/style/project-001/styled.json");
        repository.save(workflow);
        List<String> executionLog = new ArrayList<>();
        WorkflowRunner runner = runner(repository, executionLog);

        runner.runWorkflow(workflow.getWorkflowId());

        assertThat(executionLog).containsExactly("review:artifacts/style/project-001/styled.json");
        assertThat(repository.findById(workflow.getWorkflowId())).get()
            .extracting(Workflow::getStatus)
            .isEqualTo(WorkflowStatus.COMPLETED);
    }

    @Test
    @DisplayName("FAILED 재실행 시 grouping artifact가 있으면 대표 사진 단계부터 재개한다")
    void resumesFromHeroWhenGroupingArtifactExists() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(UUID.randomUUID(), "project-001", "LOCATION_BASED", 90, WorkflowStatus.FAILED);
        workflow.recordPhotoInfoArtifacts(2, "artifacts/photo-info/project-001/bundle.json", null);
        workflow.recordGroupingArtifacts(1, "artifacts/photo-grouping/project-001/grouping-result.json");
        repository.save(workflow);
        List<String> executionLog = new ArrayList<>();
        WorkflowRunner runner = runner(repository, executionLog);

        runner.runWorkflow(workflow.getWorkflowId());

        assertThat(executionLog).containsExactly(
            "hero-photo:artifacts/photo-grouping/project-001/grouping-result.json",
            "outline:artifacts/hero-photo/project-001/hero-result.json",
            "draft:artifacts/outline/project-001/outline.json",
            "style:artifacts/draft/project-001/draft.json",
            "review:artifacts/style/project-001/styled.json"
        );
    }

    @Test
    @DisplayName("FAILED 재실행 시 photo-info artifact가 있으면 grouping 단계부터 재개한다")
    void resumesFromGroupingWhenPhotoInfoArtifactExists() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(UUID.randomUUID(), "project-001", "LOCATION_BASED", 90, WorkflowStatus.FAILED);
        workflow.recordPhotoInfoArtifacts(2, "artifacts/photo-info/project-001/bundle.json", null);
        repository.save(workflow);
        List<String> executionLog = new ArrayList<>();
        WorkflowRunner runner = runner(repository, executionLog);

        runner.runWorkflow(workflow.getWorkflowId());

        assertThat(executionLog).containsExactly(
            "privacy:artifacts/photo-info/project-001/bundle.json",
            "quality:artifacts/privacy/project-001/bundle.json",
            "photo-grouping:LOCATION_BASED:artifacts/quality/project-001/bundle.json",
            "hero-photo:artifacts/photo-grouping/project-001/grouping-result.json",
            "outline:artifacts/hero-photo/project-001/hero-result.json",
            "draft:artifacts/outline/project-001/outline.json",
            "style:artifacts/draft/project-001/draft.json",
            "review:artifacts/style/project-001/styled.json"
        );
    }

    @Test
    @DisplayName("FAILED 재실행 시 privacy artifact가 있으면 quality 단계부터 재개한다")
    void resumesFromQualityWhenPrivacyArtifactExists() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(UUID.randomUUID(), "project-001", "LOCATION_BASED", 90, WorkflowStatus.FAILED);
        workflow.recordPhotoInfoArtifacts(2, "artifacts/photo-info/project-001/bundle.json", null);
        workflow.recordPrivacyArtifacts(
            2,
            0,
            "artifacts/privacy/project-001/privacy-result.json",
            "artifacts/privacy/project-001/bundle.json"
        );
        repository.save(workflow);
        List<String> executionLog = new ArrayList<>();
        WorkflowRunner runner = runner(repository, executionLog);

        runner.runWorkflow(workflow.getWorkflowId());

        assertThat(executionLog).containsExactly(
            "quality:artifacts/privacy/project-001/bundle.json",
            "photo-grouping:LOCATION_BASED:artifacts/quality/project-001/bundle.json",
            "hero-photo:artifacts/photo-grouping/project-001/grouping-result.json",
            "outline:artifacts/hero-photo/project-001/hero-result.json",
            "draft:artifacts/outline/project-001/outline.json",
            "style:artifacts/draft/project-001/draft.json",
            "review:artifacts/style/project-001/styled.json"
        );
    }

    @Test
    @DisplayName("실행 중인 워크플로는 중복 실행을 거부한다")
    void rejectsAlreadyRunningWorkflow() {
        InMemoryWorkflowRepositoryStub repository = new InMemoryWorkflowRepositoryStub();
        Workflow workflow = new Workflow(UUID.randomUUID(), "project-001", "LOCATION_BASED", 90, WorkflowStatus.REVIEWING);
        repository.save(workflow);
        WorkflowRunner runner = runner(repository, new ArrayList<>());

        assertThatThrownBy(() -> runner.runWorkflow(workflow.getWorkflowId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Workflow is already running");
    }

    @Test
    @DisplayName("존재하지 않는 워크플로 실행 요청은 실패한다")
    void failsWhenWorkflowIsMissing() {
        WorkflowRunner runner = runner(new InMemoryWorkflowRepositoryStub(), new ArrayList<>());

        assertThatThrownBy(() -> runner.runWorkflow(UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Workflow not found");
    }

    private WorkflowRunner runner(InMemoryWorkflowRepositoryStub repository, List<String> executionLog) {
        return new WorkflowRunner(
            repository,
            new WorkflowStateMachine(),
            projectId -> {
                executionLog.add("photo-info:%s".formatted(projectId));
                return new PhotoInfoResult(10, "artifacts/photo-info/project-001/bundle.json", "artifacts/photo-info/project-001/blog.md");
            },
            (projectId, photoInfoResult) -> {
                executionLog.add("privacy:%s".formatted(photoInfoResult.bundlePath()));
                return new PrivacySafetyResult(
                    photoInfoResult.photoCount(),
                    0,
                    "artifacts/privacy/project-001/privacy-result.json",
                    "artifacts/privacy/project-001/bundle.json"
                );
            },
            (projectId, photoInfoResult) -> {
                executionLog.add("quality:%s".formatted(photoInfoResult.bundlePath()));
                return new QualityScoreResult(
                    photoInfoResult.photoCount(),
                    0.75,
                    "artifacts/quality/project-001/quality-result.json",
                    "artifacts/quality/project-001/bundle.json"
                );
            },
            (projectId, groupingStrategy, timeWindowMinutes, photoInfoResult) -> {
                executionLog.add("photo-grouping:%s:%s".formatted(groupingStrategy, photoInfoResult.bundlePath()));
                return new PhotoGroupingResult("LOCATION_BASED", 3, "artifacts/photo-grouping/project-001/grouping-result.json");
            },
            (projectId, photoInfoResult, photoGroupingResult) -> {
                executionLog.add("hero-photo:%s".formatted(photoGroupingResult.resultPath()));
                return new HeroPhotoResult(3, "artifacts/hero-photo/project-001/hero-result.json");
            },
            (projectId, photoInfoResult, photoGroupingResult, heroPhotoResult) -> {
                executionLog.add("outline:%s".formatted(heroPhotoResult.resultPath()));
                return new OutlineResult(4, "artifacts/outline/project-001/outline.json");
            },
            (projectId, photoInfoResult, photoGroupingResult, heroPhotoResult, outlineResult) -> {
                executionLog.add("draft:%s".formatted(outlineResult.resultPath()));
                return new DraftResult(4, "artifacts/draft/project-001/draft.json");
            },
            (projectId, draftResult, voiceProfileId) -> {
                executionLog.add("style:%s".formatted(draftResult.resultPath()));
                return new StyleResult(150, "artifacts/style/project-001/styled.json");
            },
            (projectId, photoInfoResult, styleResult) -> {
                executionLog.add("review:%s".formatted(styleResult.resultPath()));
                return new ReviewResult(0, "artifacts/review/project-001/final.json");
            }
        );
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
