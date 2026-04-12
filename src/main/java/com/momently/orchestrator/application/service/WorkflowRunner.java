package com.momently.orchestrator.application.service;

import com.momently.orchestrator.application.port.in.RunWorkflowUseCase;
import com.momently.orchestrator.application.port.out.PhotoGroupingAgentPort;
import com.momently.orchestrator.application.port.out.PhotoInfoAgentPort;
import com.momently.orchestrator.application.port.out.WorkflowRepository;
import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 정해진 순서대로 에이전트를 호출하는 워크플로 실행기다.
 */
@Service
public class WorkflowRunner implements RunWorkflowUseCase {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStateMachine workflowStateMachine;
    private final PhotoInfoAgentPort photoInfoAgentPort;
    private final PhotoGroupingAgentPort photoGroupingAgentPort;

    /**
     * 워크플로 실행기에 필요한 의존성을 생성한다.
     *
     * @param workflowRepository 워크플로 저장 포트
     * @param workflowStateMachine 상태 전이 규칙
     * @param photoInfoAgentPort 사진 정보 추출 에이전트 포트
     * @param photoGroupingAgentPort 사진 그룹화 에이전트 포트
     */
    public WorkflowRunner(
        WorkflowRepository workflowRepository,
        WorkflowStateMachine workflowStateMachine,
        PhotoInfoAgentPort photoInfoAgentPort,
        PhotoGroupingAgentPort photoGroupingAgentPort
    ) {
        this.workflowRepository = workflowRepository;
        this.workflowStateMachine = workflowStateMachine;
        this.photoInfoAgentPort = photoInfoAgentPort;
        this.photoGroupingAgentPort = photoGroupingAgentPort;
    }

    @Override
    public Workflow runWorkflow(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        try {
            advance(workflow, WorkflowStatus.PHOTO_INFO_EXTRACTING);
            photoInfoAgentPort.extractPhotoInfo(workflow.getProjectId());
            advance(workflow, WorkflowStatus.PHOTO_INFO_EXTRACTED);

            advance(workflow, WorkflowStatus.PHOTO_GROUPING);
            photoGroupingAgentPort.groupPhotos(buildGroupingPayload(workflow));
            advance(workflow, WorkflowStatus.PHOTO_GROUPED);
            return workflow;
        } catch (RuntimeException exception) {
            workflow.markFailed(workflow.getStatus().name(), exception.getMessage());
            workflowRepository.save(workflow);
            throw exception;
        }
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

    /**
     * 그룹화 에이전트에 전달할 최소 요청 본문을 구성한다.
     *
     * @param workflow 대상 워크플로
     * @return 그룹화 요청 페이로드
     */
    private Map<String, Object> buildGroupingPayload(Workflow workflow) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project_id", workflow.getProjectId());
        payload.put("grouping_strategy", workflow.getGroupingStrategy());
        return payload;
    }
}
