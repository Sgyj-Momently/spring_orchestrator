package com.momently.orchestrator.application.service;

import com.momently.orchestrator.application.port.in.RunWorkflowUseCase;
import com.momently.orchestrator.application.port.out.DraftAgentPort;
import com.momently.orchestrator.application.port.out.HeroPhotoAgentPort;
import com.momently.orchestrator.application.port.out.OutlineAgentPort;
import com.momently.orchestrator.application.port.out.PhotoGroupingAgentPort;
import com.momently.orchestrator.application.port.out.PhotoInfoAgentPort;
import com.momently.orchestrator.application.port.out.ReviewAgentPort;
import com.momently.orchestrator.application.port.out.StyleAgentPort;
import com.momently.orchestrator.application.port.out.WorkflowRepository;
import com.momently.orchestrator.application.port.out.result.DraftResult;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.ReviewResult;
import com.momently.orchestrator.application.port.out.result.StyleResult;
import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 정해진 순서대로 에이전트를 호출하는 워크플로 실행기다.
 *
 * <p>실행은 비동기로 처리된다. 호출 즉시 반환하며, 실행 결과는 워크플로 상태를 통해 확인한다.
 * FAILED 상태에서 재실행 시 이미 완료된 단계의 아티팩트를 재사용해 불필요한 LLM 호출을 줄인다.</p>
 */
@Service
public class WorkflowRunner implements RunWorkflowUseCase {

    private static final Set<WorkflowStatus> IN_PROGRESS_STATUSES = EnumSet.of(
        WorkflowStatus.PHOTO_INFO_EXTRACTING,
        WorkflowStatus.PHOTO_GROUPING,
        WorkflowStatus.HERO_PHOTO_SELECTING,
        WorkflowStatus.OUTLINE_CREATING,
        WorkflowStatus.DRAFT_CREATING,
        WorkflowStatus.STYLE_APPLYING,
        WorkflowStatus.REVIEWING
    );

    private final WorkflowRepository workflowRepository;
    private final WorkflowStateMachine workflowStateMachine;
    private final PhotoInfoAgentPort photoInfoAgentPort;
    private final PhotoGroupingAgentPort photoGroupingAgentPort;
    private final HeroPhotoAgentPort heroPhotoAgentPort;
    private final OutlineAgentPort outlineAgentPort;
    private final DraftAgentPort draftAgentPort;
    private final StyleAgentPort styleAgentPort;
    private final ReviewAgentPort reviewAgentPort;

    /**
     * 워크플로 실행기에 필요한 의존성을 생성한다.
     *
     * @param workflowRepository 워크플로 저장 포트
     * @param workflowStateMachine 상태 전이 규칙
     * @param photoInfoAgentPort 사진 정보 추출 에이전트 포트
     * @param photoGroupingAgentPort 사진 그룹화 에이전트 포트
     * @param heroPhotoAgentPort 대표 사진 선택 에이전트 포트
     * @param outlineAgentPort 개요 생성 에이전트 포트
     */
    public WorkflowRunner(
        WorkflowRepository workflowRepository,
        WorkflowStateMachine workflowStateMachine,
        PhotoInfoAgentPort photoInfoAgentPort,
        PhotoGroupingAgentPort photoGroupingAgentPort,
        HeroPhotoAgentPort heroPhotoAgentPort,
        OutlineAgentPort outlineAgentPort,
        DraftAgentPort draftAgentPort,
        StyleAgentPort styleAgentPort,
        ReviewAgentPort reviewAgentPort
    ) {
        this.workflowRepository = workflowRepository;
        this.workflowStateMachine = workflowStateMachine;
        this.photoInfoAgentPort = photoInfoAgentPort;
        this.photoGroupingAgentPort = photoGroupingAgentPort;
        this.heroPhotoAgentPort = heroPhotoAgentPort;
        this.outlineAgentPort = outlineAgentPort;
        this.draftAgentPort = draftAgentPort;
        this.styleAgentPort = styleAgentPort;
        this.reviewAgentPort = reviewAgentPort;
    }

    /**
     * 워크플로를 비동기로 실행한다.
     *
     * <p>이미 실행 중인 워크플로에 대한 중복 실행 요청은 즉시 거부한다.
     * FAILED 상태에서 재실행 시 완료된 단계의 아티팩트를 재사용해 비용이 큰 Ollama 호출을 반복하지 않는다.</p>
     *
     * @param workflowId 워크플로 식별자
     * @throws IllegalArgumentException 워크플로를 찾을 수 없는 경우
     * @throws IllegalStateException 이미 실행 중인 경우
     */
    @Async
    @Override
    public void runWorkflow(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        if (IN_PROGRESS_STATUSES.contains(workflow.getStatus())) {
            throw new IllegalStateException(
                "Workflow is already running: workflowId=" + workflowId + ", status=" + workflow.getStatus()
            );
        }

        try {
            runFromResumePoint(workflow);
        } catch (RuntimeException exception) {
            workflow.markFailed(workflow.getStatus().name(), exception.getMessage());
            workflowRepository.save(workflow);
            throw exception;
        }
    }

    /**
     * 완료된 아티팩트를 기준으로 재개 지점을 결정하고 실행한다.
     *
     * <p>아티팩트 경로가 이미 기록되어 있으면 해당 단계는 건너뛴다.
     * groupingResultPath → hero photo 단계부터,
     * photoInfoBundlePath → grouping 단계부터,
     * 둘 다 없으면 처음부터 실행한다.</p>
     */
    private void runFromResumePoint(Workflow workflow) {
        if (workflow.getReviewResultPath() != null || workflow.getStatus() == WorkflowStatus.COMPLETED) {
            return;
        }

        if (workflow.getStyleResultPath() != null) {
            PhotoInfoResult photoInfoResult = currentPhotoInfoResult(workflow);
            StyleResult styleResult = new StyleResult(
                workflow.getStyledWordCount() == null ? 0 : workflow.getStyledWordCount(),
                workflow.getStyleResultPath()
            );
            runReviewStep(workflow, photoInfoResult, styleResult);
            return;
        }

        if (workflow.getDraftResultPath() != null) {
            PhotoInfoResult photoInfoResult = currentPhotoInfoResult(workflow);
            DraftResult draftResult = new DraftResult(
                workflow.getDraftSectionCount() == null ? 0 : workflow.getDraftSectionCount(),
                workflow.getDraftResultPath()
            );
            StyleResult styleResult = runStyleStep(workflow, draftResult);
            runReviewStep(workflow, photoInfoResult, styleResult);
            return;
        }

        if (workflow.getOutlineResultPath() != null) {
            PhotoInfoResult photoInfoResult = currentPhotoInfoResult(workflow);
            OutlineResult outlineResult = new OutlineResult(
                workflow.getOutlineSectionCount() == null ? 0 : workflow.getOutlineSectionCount(),
                workflow.getOutlineResultPath()
            );
            PhotoGroupingResult photoGroupingResult = currentGroupingResult(workflow);
            HeroPhotoResult heroPhotoResult = currentHeroPhotoResult(workflow);
            runTailSteps(workflow, photoInfoResult, photoGroupingResult, heroPhotoResult, outlineResult);
            return;
        }

        if (workflow.getGroupingResultPath() != null) {
            PhotoInfoResult photoInfoResult = currentPhotoInfoResult(workflow);
            PhotoGroupingResult photoGroupingResult = currentGroupingResult(workflow);
            HeroPhotoResult heroPhotoResult = runHeroPhotoStep(workflow, photoInfoResult, photoGroupingResult);
            OutlineResult outlineResult = runOutlineStep(workflow, photoInfoResult, photoGroupingResult, heroPhotoResult);
            runTailSteps(workflow, photoInfoResult, photoGroupingResult, heroPhotoResult, outlineResult);
            return;
        }

        if (workflow.getPhotoInfoBundlePath() != null) {
            PhotoInfoResult photoInfoResult = currentPhotoInfoResult(workflow);
            PhotoGroupingResult photoGroupingResult = runGroupingStep(workflow, photoInfoResult);
            HeroPhotoResult heroPhotoResult = runHeroPhotoStep(workflow, photoInfoResult, photoGroupingResult);
            OutlineResult outlineResult = runOutlineStep(workflow, photoInfoResult, photoGroupingResult, heroPhotoResult);
            runTailSteps(workflow, photoInfoResult, photoGroupingResult, heroPhotoResult, outlineResult);
            return;
        }

        PhotoInfoResult photoInfoResult = runPhotoInfoStep(workflow);
        PhotoGroupingResult photoGroupingResult = runGroupingStep(workflow, photoInfoResult);
        HeroPhotoResult heroPhotoResult = runHeroPhotoStep(workflow, photoInfoResult, photoGroupingResult);
        OutlineResult outlineResult = runOutlineStep(workflow, photoInfoResult, photoGroupingResult, heroPhotoResult);
        runTailSteps(workflow, photoInfoResult, photoGroupingResult, heroPhotoResult, outlineResult);
    }

    private PhotoInfoResult runPhotoInfoStep(Workflow workflow) {
        advance(workflow, WorkflowStatus.PHOTO_INFO_EXTRACTING);
        PhotoInfoResult photoInfoResult = photoInfoAgentPort.extractPhotoInfo(workflow.getProjectId());
        workflow.recordPhotoInfoArtifacts(
            photoInfoResult.photoCount(),
            photoInfoResult.bundlePath(),
            photoInfoResult.blogPath()
        );
        advance(workflow, WorkflowStatus.PHOTO_INFO_EXTRACTED);
        return photoInfoResult;
    }

    private PhotoGroupingResult runGroupingStep(Workflow workflow, PhotoInfoResult photoInfoResult) {
        advance(workflow, WorkflowStatus.PHOTO_GROUPING);
        PhotoGroupingResult photoGroupingResult = photoGroupingAgentPort.groupPhotos(
            workflow.getProjectId(),
            workflow.getGroupingStrategy(),
            workflow.getTimeWindowMinutes(),
            photoInfoResult
        );
        workflow.recordGroupingArtifacts(
            photoGroupingResult.groupCount(),
            photoGroupingResult.resultPath()
        );
        advance(workflow, WorkflowStatus.PHOTO_GROUPED);
        return photoGroupingResult;
    }

    private HeroPhotoResult runHeroPhotoStep(
        Workflow workflow,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult
    ) {
        advance(workflow, WorkflowStatus.HERO_PHOTO_SELECTING);
        HeroPhotoResult heroPhotoResult = heroPhotoAgentPort.selectHeroPhotos(
            workflow.getProjectId(),
            photoInfoResult,
            photoGroupingResult
        );
        workflow.recordHeroPhotoArtifacts(heroPhotoResult.heroPhotoCount(), heroPhotoResult.resultPath());
        advance(workflow, WorkflowStatus.HERO_PHOTO_SELECTED);
        return heroPhotoResult;
    }

    private OutlineResult runOutlineStep(
        Workflow workflow,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult,
        HeroPhotoResult heroPhotoResult
    ) {
        advance(workflow, WorkflowStatus.OUTLINE_CREATING);
        OutlineResult outlineResult = outlineAgentPort.createOutline(
            workflow.getProjectId(),
            photoInfoResult,
            photoGroupingResult,
            heroPhotoResult
        );
        workflow.recordOutlineArtifacts(outlineResult.outlineSectionCount(), outlineResult.resultPath());
        advance(workflow, WorkflowStatus.OUTLINE_CREATED);
        return outlineResult;
    }

    private void runTailSteps(
        Workflow workflow,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult,
        HeroPhotoResult heroPhotoResult,
        OutlineResult outlineResult
    ) {
        DraftResult draftResult = runDraftStep(
            workflow,
            photoInfoResult,
            photoGroupingResult,
            heroPhotoResult,
            outlineResult
        );
        StyleResult styleResult = runStyleStep(workflow, draftResult);
        runReviewStep(workflow, photoInfoResult, styleResult);
    }

    private DraftResult runDraftStep(
        Workflow workflow,
        PhotoInfoResult photoInfoResult,
        PhotoGroupingResult photoGroupingResult,
        HeroPhotoResult heroPhotoResult,
        OutlineResult outlineResult
    ) {
        advance(workflow, WorkflowStatus.DRAFT_CREATING);
        DraftResult draftResult = draftAgentPort.createDraft(
            workflow.getProjectId(),
            photoInfoResult,
            photoGroupingResult,
            heroPhotoResult,
            outlineResult
        );
        workflow.recordDraftArtifacts(draftResult.draftSectionCount(), draftResult.resultPath());
        advance(workflow, WorkflowStatus.DRAFT_CREATED);
        return draftResult;
    }

    private StyleResult runStyleStep(Workflow workflow, DraftResult draftResult) {
        advance(workflow, WorkflowStatus.STYLE_APPLYING);
        StyleResult styleResult = styleAgentPort.applyStyle(workflow.getProjectId(), draftResult);
        workflow.recordStyleArtifacts(styleResult.wordCount(), styleResult.resultPath());
        advance(workflow, WorkflowStatus.STYLE_APPLIED);
        return styleResult;
    }

    private void runReviewStep(Workflow workflow, PhotoInfoResult photoInfoResult, StyleResult styleResult) {
        advance(workflow, WorkflowStatus.REVIEWING);
        ReviewResult reviewResult = reviewAgentPort.reviewDocument(workflow.getProjectId(), photoInfoResult, styleResult);
        workflow.recordReviewArtifacts(reviewResult.issueCount(), reviewResult.resultPath());
        advance(workflow, WorkflowStatus.REVIEW_COMPLETED);
        advance(workflow, WorkflowStatus.COMPLETED);
    }

    private PhotoInfoResult currentPhotoInfoResult(Workflow workflow) {
        return new PhotoInfoResult(
            workflow.getPhotoCount() == null ? 0 : workflow.getPhotoCount(),
            workflow.getPhotoInfoBundlePath(),
            workflow.getBlogPath()
        );
    }

    private PhotoGroupingResult currentGroupingResult(Workflow workflow) {
        return new PhotoGroupingResult(
            workflow.getGroupingStrategy(),
            workflow.getGroupCount() == null ? 0 : workflow.getGroupCount(),
            workflow.getGroupingResultPath()
        );
    }

    private HeroPhotoResult currentHeroPhotoResult(Workflow workflow) {
        return new HeroPhotoResult(
            workflow.getHeroPhotoCount() == null ? 0 : workflow.getHeroPhotoCount(),
            workflow.getHeroPhotoResultPath()
        );
    }

    /**
     * 상태 머신 검증을 통과한 상태만 저장한다.
     *
     * @param workflow 대상 워크플로
     * @param nextStatus 다음 상태
     */
    private void advance(Workflow workflow, WorkflowStatus nextStatus) {
        workflowStateMachine.transition(workflow, nextStatus);
        workflowRepository.save(workflow);
    }
}
