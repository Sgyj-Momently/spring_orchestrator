package com.momently.orchestrator.application.service;

import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 워크플로 상태 전이 규칙을 관리하는 상태 머신이다.
 */
@Component
public class WorkflowStateMachine {

    private final Map<WorkflowStatus, Set<WorkflowStatus>> allowedTransitions;

    /**
     * 문서에 정의된 순차 전이 규칙을 초기화한다.
     */
    public WorkflowStateMachine() {
        allowedTransitions = new EnumMap<>(WorkflowStatus.class);
        allowedTransitions.put(WorkflowStatus.CREATED, EnumSet.of(WorkflowStatus.PHOTO_INFO_EXTRACTING));
        allowedTransitions.put(
            WorkflowStatus.PHOTO_INFO_EXTRACTING,
            EnumSet.of(WorkflowStatus.PHOTO_INFO_EXTRACTED)
        );
        allowedTransitions.put(WorkflowStatus.PHOTO_INFO_EXTRACTED, EnumSet.of(WorkflowStatus.PHOTO_GROUPING));
        allowedTransitions.put(WorkflowStatus.PHOTO_GROUPING, EnumSet.of(WorkflowStatus.PHOTO_GROUPED));
        allowedTransitions.put(
            WorkflowStatus.PHOTO_GROUPED,
            EnumSet.of(WorkflowStatus.HERO_PHOTO_SELECTING)
        );
        allowedTransitions.put(
            WorkflowStatus.HERO_PHOTO_SELECTING,
            EnumSet.of(WorkflowStatus.HERO_PHOTO_SELECTED)
        );
        allowedTransitions.put(
            WorkflowStatus.HERO_PHOTO_SELECTED,
            EnumSet.of(WorkflowStatus.OUTLINE_CREATING)
        );
        allowedTransitions.put(
            WorkflowStatus.OUTLINE_CREATING,
            EnumSet.of(WorkflowStatus.OUTLINE_CREATED)
        );
        allowedTransitions.put(WorkflowStatus.OUTLINE_CREATED, EnumSet.of(WorkflowStatus.DRAFT_CREATING));
        allowedTransitions.put(WorkflowStatus.DRAFT_CREATING, EnumSet.of(WorkflowStatus.DRAFT_CREATED));
        allowedTransitions.put(WorkflowStatus.DRAFT_CREATED, EnumSet.of(WorkflowStatus.STYLE_APPLYING));
        allowedTransitions.put(WorkflowStatus.STYLE_APPLYING, EnumSet.of(WorkflowStatus.STYLE_APPLIED));
        allowedTransitions.put(WorkflowStatus.STYLE_APPLIED, EnumSet.of(WorkflowStatus.REVIEWING));
        allowedTransitions.put(WorkflowStatus.REVIEWING, EnumSet.of(WorkflowStatus.REVIEW_COMPLETED));
        allowedTransitions.put(WorkflowStatus.REVIEW_COMPLETED, EnumSet.of(WorkflowStatus.COMPLETED));
        allowedTransitions.put(
            WorkflowStatus.FAILED,
            EnumSet.of(
                WorkflowStatus.PHOTO_INFO_EXTRACTING,
                WorkflowStatus.PHOTO_GROUPING,
                WorkflowStatus.HERO_PHOTO_SELECTING,
                WorkflowStatus.OUTLINE_CREATING,
                WorkflowStatus.DRAFT_CREATING,
                WorkflowStatus.STYLE_APPLYING,
                WorkflowStatus.REVIEWING
            )
        );
        allowedTransitions.put(WorkflowStatus.COMPLETED, EnumSet.noneOf(WorkflowStatus.class));
    }

    /**
     * 현재 상태에서 다음 상태로 전이 가능한지 확인한다.
     *
     * @param currentStatus 현재 상태
     * @param nextStatus 다음 상태
     * @return 전이 가능 여부
     */
    public boolean canTransition(WorkflowStatus currentStatus, WorkflowStatus nextStatus) {
        if (nextStatus == WorkflowStatus.FAILED) {
            return currentStatus != WorkflowStatus.COMPLETED && currentStatus != WorkflowStatus.FAILED;
        }
        return allowedTransitions.getOrDefault(currentStatus, Set.of()).contains(nextStatus);
    }

    /**
     * 상태 전이 규칙을 검증한 뒤 워크플로에 반영한다.
     *
     * @param workflow 대상 워크플로
     * @param nextStatus 다음 상태
     * @return 상태가 변경된 워크플로
     */
    public Workflow transition(Workflow workflow, WorkflowStatus nextStatus) {
        WorkflowStatus currentStatus = workflow.getStatus();
        if (!canTransition(currentStatus, nextStatus)) {
            throw new IllegalStateException(
                "허용되지 않은 상태 전이입니다. current=%s, next=%s".formatted(currentStatus, nextStatus)
            );
        }
        workflow.updateStatus(nextStatus);
        return workflow;
    }
}
