package com.momently.orchestrator.application.service;

import com.momently.orchestrator.application.port.in.RunWorkflowUseCase;
import com.momently.orchestrator.application.port.out.HeroPhotoAgentPort;
import com.momently.orchestrator.application.port.out.OutlineAgentPort;
import com.momently.orchestrator.application.port.out.PhotoGroupingAgentPort;
import com.momently.orchestrator.application.port.out.PhotoInfoAgentPort;
import com.momently.orchestrator.application.port.out.WorkflowRepository;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.OutlineResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
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
        WorkflowStatus.HERO_PHOTO_SELECTING
    );

    private final WorkflowRepository workflowRepository;
    private final WorkflowStateMachine workflowStateMachine;
    private final PhotoInfoAgentPort photoInfoAgentPort;
    private final PhotoGroupingAgentPort photoGroupingAgentPort;
    private final HeroPhotoAgentPort heroPhotoAgentPort;
    private final OutlineAgentPort outlineAgentPort;

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
        OutlineAgentPort outlineAgentPort
    ) {
        this.workflowRepository = workflowRepository;
        this.workflowStateMachine = workflowStateMachine;
        this.photoInfoAgentPort = photoInfoAgentPort;
        this.photoGroupingAgentPort = photoGroupingAgentPort;
        this.heroPhotoAgentPort = heroPhotoAgentPort;
        this.outlineAgentPort = outlineAgentPort;
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
        if (workflow.getOutlineResultPath() != null) {
            return;
        }

        if (workflow.getGroupingResultPath() != null) {
            PhotoInfoResult photoInfoResult = new PhotoInfoResult(
                workflow.getPhotoCount(),
                workflow.getPhotoInfoBundlePath(),
                workflow.getBlogPath()
            );
            PhotoGroupingResult photoGroupingResult = new PhotoGroupingResult(
                workflow.getGroupingStrategy(),
                workflow.getGroupCount(),
                workflow.getGroupingResultPath()
            );
            HeroPhotoResult heroPhotoResult = runHeroPhotoStep(workflow, photoInfoResult, photoGroupingResult);
            runOutlineStep(workflow, photoInfoResult, photoGroupingResult, heroPhotoResult);
            return;
        }

        if (workflow.getPhotoInfoBundlePath() != null) {
            PhotoInfoResult photoInfoResult = new PhotoInfoResult(
                workflow.getPhotoCount(),
                workflow.getPhotoInfoBundlePath(),
                workflow.getBlogPath()
            );
            PhotoGroupingResult photoGroupingResult = runGroupingStep(workflow, photoInfoResult);
            HeroPhotoResult heroPhotoResult = runHeroPhotoStep(workflow, photoInfoResult, photoGroupingResult);
            runOutlineStep(workflow, photoInfoResult, photoGroupingResult, heroPhotoResult);
            return;
        }

        PhotoInfoResult photoInfoResult = runPhotoInfoStep(workflow);
        PhotoGroupingResult photoGroupingResult = runGroupingStep(workflow, photoInfoResult);
        HeroPhotoResult heroPhotoResult = runHeroPhotoStep(workflow, photoInfoResult, photoGroupingResult);
        runOutlineStep(workflow, photoInfoResult, photoGroupingResult, heroPhotoResult);
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

    private void runOutlineStep(
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
