package com.momently.orchestrator.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JPA entity와 도메인 워크플로 간 매핑 계약을 검증한다.
 */
class WorkflowJpaEntityTest {

    @Test
    @DisplayName("워크플로 실행 메타데이터와 아티팩트 경로를 도메인으로 왕복 변환한다")
    void convertsWorkflowArtifactsRoundTrip() {
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-001",
            "LOCATION_BASED",
            90,
            "voice-family",
            WorkflowStatus.COMPLETED
        );
        workflow.recordPhotoInfoArtifacts(12, "output/project-001/bundles/bundle.json", "output/project-001/blog.md");
        workflow.recordPrivacyArtifacts(10, 2, "output/project-001/privacy/result.json", "output/project-001/privacy/public-bundle.json");
        workflow.recordQualityScoreArtifacts(10, 0.82, "output/project-001/quality/result.json", "output/project-001/quality/scored-bundle.json");
        workflow.recordGroupingArtifacts(4, "output/project-001/grouping/result.json");
        workflow.recordHeroPhotoArtifacts(4, "output/project-001/hero/result.json");
        workflow.recordOutlineArtifacts(5, "output/project-001/outline/result.json");
        workflow.recordDraftArtifacts(5, "output/project-001/draft/result.json");
        workflow.recordStyleArtifacts(1_240, "output/project-001/style/result.md");
        workflow.recordReviewArtifacts(1, "output/project-001/review/result.json");

        WorkflowJpaEntity entity = WorkflowJpaEntity.fromDomain(workflow);
        entity.prePersist();
        Workflow converted = entity.toDomain();

        assertThat(converted.getWorkflowId()).isEqualTo(workflow.getWorkflowId());
        assertThat(converted.getProjectId()).isEqualTo("project-001");
        assertThat(converted.getGroupingStrategy()).isEqualTo("LOCATION_BASED");
        assertThat(converted.getTimeWindowMinutes()).isEqualTo(90);
        assertThat(converted.getVoiceProfileId()).isEqualTo("voice-family");
        assertThat(converted.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(converted.getPhotoCount()).isEqualTo(10);
        assertThat(converted.getPrivacyExcludedCount()).isEqualTo(2);
        assertThat(converted.getAverageQualityScore()).isEqualTo(0.82);
        assertThat(converted.getGroupCount()).isEqualTo(4);
        assertThat(converted.getHeroPhotoCount()).isEqualTo(4);
        assertThat(converted.getOutlineSectionCount()).isEqualTo(5);
        assertThat(converted.getDraftSectionCount()).isEqualTo(5);
        assertThat(converted.getStyledWordCount()).isEqualTo(1_240);
        assertThat(converted.getReviewIssueCount()).isEqualTo(1);
        assertThat(converted.getPhotoInfoBundlePath()).isEqualTo("output/project-001/bundles/bundle.json");
        assertThat(converted.getPrivacyResultPath()).isEqualTo("output/project-001/privacy/result.json");
        assertThat(converted.getPrivacyBundlePath()).isEqualTo("output/project-001/privacy/public-bundle.json");
        assertThat(converted.getQualityScoreResultPath()).isEqualTo("output/project-001/quality/result.json");
        assertThat(converted.getQualityScoreBundlePath()).isEqualTo("output/project-001/quality/scored-bundle.json");
        assertThat(converted.getBlogPath()).isEqualTo("output/project-001/blog.md");
        assertThat(converted.getGroupingResultPath()).isEqualTo("output/project-001/grouping/result.json");
        assertThat(converted.getHeroPhotoResultPath()).isEqualTo("output/project-001/hero/result.json");
        assertThat(converted.getOutlineResultPath()).isEqualTo("output/project-001/outline/result.json");
        assertThat(converted.getDraftResultPath()).isEqualTo("output/project-001/draft/result.json");
        assertThat(converted.getStyleResultPath()).isEqualTo("output/project-001/style/result.md");
        assertThat(converted.getReviewResultPath()).isEqualTo("output/project-001/review/result.json");
    }

    @Test
    @DisplayName("실패 상태와 마지막 오류 정보를 도메인으로 복원한다")
    void convertsFailureMetadata() {
        Workflow workflow = new Workflow(
            UUID.randomUUID(),
            "project-002",
            "TIME_BASED",
            45,
            WorkflowStatus.PHOTO_GROUPING
        );
        workflow.markFailed("PHOTO_GROUPING", "agent timeout");

        Workflow converted = WorkflowJpaEntity.fromDomain(workflow).toDomain();

        assertThat(converted.getStatus()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(converted.getLastFailedStep()).isEqualTo("PHOTO_GROUPING");
        assertThat(converted.getLastErrorMessage()).isEqualTo("agent timeout");
    }

    @Test
    @DisplayName("JPA 생명주기 콜백은 생성/수정 시간을 채운다")
    void updatesTimestampsFromLifecycleCallbacks() {
        WorkflowJpaEntity entity = WorkflowJpaEntity.fromDomain(new Workflow(
            UUID.randomUUID(),
            "project-003",
            "SCENE_BASED",
            30,
            WorkflowStatus.CREATED
        ));

        entity.prePersist();
        entity.preUpdate();

        assertThat(entity.toDomain().getProjectId()).isEqualTo("project-003");
    }
}
