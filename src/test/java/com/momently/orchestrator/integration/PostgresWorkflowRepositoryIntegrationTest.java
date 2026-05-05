package com.momently.orchestrator.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.momently.orchestrator.application.port.out.WorkflowRepository;
import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PostgreSQL 프로필이 실제 Postgres에 워크플로 메타데이터를 저장하고 복원하는지 검증한다.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"postgres", "stub-agents"})
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_INTEGRATION_TESTS", matches = "true")
class PostgresWorkflowRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private WorkflowRepository repository;

    @Test
    @DisplayName("PostgreSQL 저장소는 워크플로 아티팩트와 실패 메타데이터를 왕복 보존한다")
    void persistsWorkflowMetadataInPostgres() {
        UUID workflowId = UUID.randomUUID();
        Workflow workflow = new Workflow(
            workflowId,
            "postgres-project",
            "TIME_BASED",
            45,
            "voice-family",
            WorkflowStatus.CREATED
        );
        workflow.recordPhotoInfoArtifacts(3, "output/postgres-project/bundles/bundle.json", null);
        workflow.recordPrivacyArtifacts(2, 1, "output/postgres-project/privacy/result.json", "output/postgres-project/privacy/bundle.json");
        workflow.recordQualityScoreArtifacts(2, 0.72, "output/postgres-project/quality/result.json", "output/postgres-project/quality/bundle.json");
        workflow.markFailed("QUALITY_SCORING", "agent timeout");

        repository.save(workflow);

        Workflow restored = repository.findById(workflowId).orElseThrow();
        assertThat(restored.getProjectId()).isEqualTo("postgres-project");
        assertThat(restored.getVoiceProfileId()).isEqualTo("voice-family");
        assertThat(restored.getStatus()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(restored.getPhotoCount()).isEqualTo(2);
        assertThat(restored.getPrivacyExcludedCount()).isEqualTo(1);
        assertThat(restored.getAverageQualityScore()).isEqualTo(0.72);
        assertThat(restored.getQualityScoreBundlePath()).isEqualTo("output/postgres-project/quality/bundle.json");
        assertThat(restored.getLastFailedStep()).isEqualTo("QUALITY_SCORING");
        assertThat(restored.getLastErrorMessage()).isEqualTo("agent timeout");
        assertThat(repository.findAll()).extracting(Workflow::getWorkflowId).contains(workflowId);
    }

    @Test
    @DisplayName("PostgreSQL 저장소는 기존 워크플로를 같은 식별자로 갱신한다")
    void updatesExistingWorkflowInPostgres() {
        UUID workflowId = UUID.randomUUID();
        Workflow workflow = new Workflow(
            workflowId,
            "postgres-update",
            "LOCATION_BASED",
            90,
            WorkflowStatus.CREATED
        );
        repository.save(workflow);

        workflow.updateStatus(WorkflowStatus.PHOTO_INFO_EXTRACTING);
        workflow.recordPhotoInfoArtifacts(5, "output/postgres-update/bundles/bundle.json", null);
        repository.save(workflow);

        Workflow restored = repository.findById(workflowId).orElseThrow();
        assertThat(restored.getStatus()).isEqualTo(WorkflowStatus.PHOTO_INFO_EXTRACTING);
        assertThat(restored.getPhotoCount()).isEqualTo(5);
        assertThat(repository.findAll())
            .filteredOn(item -> item.getWorkflowId().equals(workflowId))
            .hasSize(1);
    }
}
