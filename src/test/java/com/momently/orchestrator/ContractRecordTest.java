package com.momently.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import com.momently.orchestrator.adapter.in.web.request.CreateWorkflowRequest;
import com.momently.orchestrator.adapter.in.web.response.WorkflowResponse;
import com.momently.orchestrator.application.port.in.command.CreateWorkflowCommand;
import com.momently.orchestrator.application.port.out.result.HeroPhotoResult;
import com.momently.orchestrator.application.port.out.result.PhotoGroupingResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

/**
 * 단순 계약 모델과 애플리케이션 진입점을 검증한다.
 */
class ContractRecordTest {

    @Test
    @DisplayName("요청/응답 및 결과 record는 전달한 값을 그대로 노출한다")
    void exposesRecordValues() {
        UUID workflowId = UUID.randomUUID();
        CreateWorkflowRequest request = new CreateWorkflowRequest("project-001", "LOCATION_BASED", 90);
        CreateWorkflowCommand command = new CreateWorkflowCommand("project-001", "LOCATION_BASED", 90);
        WorkflowResponse response = new WorkflowResponse(
            workflowId,
            "project-001",
            "LOCATION_BASED",
            WorkflowStatus.CREATED,
            10,
            3,
            2,
            4,
            "bundle.json",
            "blog.md",
            "grouping-result.json",
            "hero-result.json",
            "outline.json",
            null,
            null
        );
        PhotoInfoResult photoInfoResult = new PhotoInfoResult(10, "bundle.json");
        PhotoGroupingResult photoGroupingResult = new PhotoGroupingResult("TIME_BASED", 3);
        HeroPhotoResult heroPhotoResult = new HeroPhotoResult(2);

        assertThat(request.projectId()).isEqualTo("project-001");
        assertThat(command.groupingStrategy()).isEqualTo("LOCATION_BASED");
        assertThat(response.workflowId()).isEqualTo(workflowId);
        assertThat(response.status()).isEqualTo(WorkflowStatus.CREATED);
        assertThat(response.photoInfoBundlePath()).isEqualTo("bundle.json");
        assertThat(response.groupingResultPath()).isEqualTo("grouping-result.json");
        assertThat(photoInfoResult.photoCount()).isEqualTo(10);
        assertThat(photoGroupingResult.groupCount()).isEqualTo(3);
        assertThat(heroPhotoResult.heroPhotoCount()).isEqualTo(2);
        assertThat(heroPhotoResult.resultPath()).isNull();
    }

    @Test
    @DisplayName("애플리케이션 메인은 SpringApplication.run을 호출한다")
    void runsSpringApplication() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            OrchestratorApplication.main(new String[] {"--spring.profiles.active=test"});

            springApplication.verify(() -> SpringApplication.run(OrchestratorApplication.class,
                new String[] {"--spring.profiles.active=test"}));
        }
    }
}
