package com.momently.orchestrator.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.momently.orchestrator.application.port.in.CreateWorkflowUseCase;
import com.momently.orchestrator.application.port.in.GetWorkflowUseCase;
import com.momently.orchestrator.application.port.in.RunWorkflowUseCase;
import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.UUID;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 워크플로 웹 어댑터의 요청/응답 계약을 검증한다.
 */
@WebMvcTest(WorkflowController.class)
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateWorkflowUseCase createWorkflowUseCase;

    @MockBean
    private GetWorkflowUseCase getWorkflowUseCase;

    @MockBean
    private RunWorkflowUseCase runWorkflowUseCase;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("워크플로 생성 요청을 받아 HATEOAS 응답을 반환한다")
    void createsWorkflow() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f10");
        Workflow workflow = new Workflow(
            workflowId,
            "project-001",
            "LOCATION_BASED",
            90,
            WorkflowStatus.CREATED
        );
        when(createWorkflowUseCase.createWorkflow(any())).thenReturn(workflow);

        mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "projectId": "project-001",
                      "groupingStrategy": "LOCATION_BASED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workflowId").value(workflowId.toString()))
            .andExpect(jsonPath("$.projectId").value("project-001"))
            .andExpect(jsonPath("$.groupingStrategy").value("LOCATION_BASED"))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$._links.self.href").exists())
            .andExpect(jsonPath("$._links.run.href").exists());
    }

    @Test
    @DisplayName("유효하지 않은 생성 요청은 400을 반환한다")
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "projectId": "",
                      "groupingStrategy": ""
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("워크플로 조회 시 HATEOAS 응답을 반환한다")
    void getsWorkflow() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f11");
        Workflow workflow = new Workflow(
            workflowId,
            "project-002",
            "TIME_BASED",
            90,
            WorkflowStatus.PHOTO_GROUPED
        );
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workflowId").value(workflowId.toString()))
            .andExpect(jsonPath("$.projectId").value("project-002"))
            .andExpect(jsonPath("$.groupingStrategy").value("TIME_BASED"))
            .andExpect(jsonPath("$.status").value("PHOTO_GROUPED"))
            .andExpect(jsonPath("$._links.self.href").exists())
            .andExpect(jsonPath("$._links.run.href").exists());
    }

    @Test
    @DisplayName("워크플로 실행 요청을 받아 202 Accepted와 상태 조회용 Location 헤더를 반환한다")
    void runsWorkflow() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f12");

        mockMvc.perform(post("/api/v1/workflows/{workflowId}/run", workflowId))
            .andExpect(status().isAccepted())
            .andExpect(header().exists("Location"));
    }

    @Test
    @DisplayName("워크플로 artifact JSON을 조회한다")
    void getsJsonArtifact() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f13");
        Path finalPath = tempDir.resolve("final.json");
        Files.writeString(finalPath, "{\"review_status\":\"ok\"}");
        Workflow workflow = new Workflow(workflowId, "project-003", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordReviewArtifacts(0, finalPath.toString());
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/review", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.artifactType").value("review"))
            .andExpect(jsonPath("$.contentType").value("application/json"))
            .andExpect(jsonPath("$.json.review_status").value("ok"));
    }

    @Test
    @DisplayName("워크플로 artifact 텍스트를 조회한다")
    void getsTextArtifact() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f14");
        Path blogPath = tempDir.resolve("blog.md");
        Files.writeString(blogPath, "# Blog");
        Workflow workflow = new Workflow(workflowId, "project-004", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordPhotoInfoArtifacts(1, tempDir.resolve("bundle.json").toString(), blogPath.toString());
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/blog", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contentType").value("text/plain"))
            .andExpect(jsonPath("$.text").value("# Blog"));
    }

    @Test
    @DisplayName("없는 artifact는 404를 반환한다")
    void returnsNotFoundForMissingArtifact() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f15");
        Workflow workflow = new Workflow(workflowId, "project-005", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/review", workflowId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("알 수 없는 artifact 타입도 404를 반환한다")
    void returnsNotFoundForUnknownArtifactType() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f16");
        Workflow workflow = new Workflow(workflowId, "project-006", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/unknown", workflowId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("경로는 있지만 파일이 없으면 404를 반환한다")
    void returnsNotFoundForMissingArtifactFile() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f17");
        Workflow workflow = new Workflow(workflowId, "project-007", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordReviewArtifacts(0, tempDir.resolve("missing.json").toString());
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/final", workflowId))
            .andExpect(status().isNotFound());
    }
}
