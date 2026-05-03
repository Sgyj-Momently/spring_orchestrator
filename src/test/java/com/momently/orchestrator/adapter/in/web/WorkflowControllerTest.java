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
import com.momently.orchestrator.security.JwtService;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.UUID;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
@AutoConfigureMockMvc(addFilters = false)
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateWorkflowUseCase createWorkflowUseCase;

    @MockBean
    private GetWorkflowUseCase getWorkflowUseCase;

    @MockBean
    private RunWorkflowUseCase runWorkflowUseCase;

    /** 슬라이스에 보안 빈 체인이 올라가므로 JwtService만 모의로 채운다. */
    @MockBean
    private JwtService jwtService;

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

    @ParameterizedTest
    @ValueSource(strings = {"privacy", "privacy-safety"})
    @DisplayName("민감정보 아티팩트 alias 요청 키가 서로 같다")
    void resolvesPrivacyAliases(String artifactKey) throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f31");
        Path result = tempDir.resolve("privacy-res.json");
        Files.writeString(result, "{\"ok\":true}");
        Workflow workflow = new Workflow(workflowId, "p", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordPrivacyArtifacts(2, 1, result.toString(), tempDir.resolve("public.json").toString());
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/{artifactType}", workflowId, artifactKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.artifactType").value(artifactKey))
            .andExpect(jsonPath("$.json.ok").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"bundle", "photo-info"})
    @DisplayName("사진 정보 번들 alias 요청 키가 서로 같다")
    void resolvesPhotoInfoAliases(String artifactKey) throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f32");
        Path bundlePath = tempDir.resolve("bundle-alias.json");
        Files.writeString(bundlePath, "{\"photos\":[]}");
        Workflow workflow = new Workflow(workflowId, "p", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordPhotoInfoArtifacts(0, bundlePath.toString(), null);
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/{artifactType}", workflowId, artifactKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.artifactType").value(artifactKey))
            .andExpect(jsonPath("$.json.photos").isArray());
    }

    @ParameterizedTest
    @ValueSource(strings = {"hero", "hero-photo"})
    @DisplayName("대표 사진 alias 요청 키가 서로 같다")
    void resolvesHeroAliases(String artifactKey) throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f33");
        Path heroPath = tempDir.resolve("hero.json");
        Files.writeString(heroPath, "{\"h\":1}");
        Workflow workflow = new Workflow(workflowId, "p", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordHeroPhotoArtifacts(1, heroPath.toString());
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/{artifactType}", workflowId, artifactKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.artifactType").value(artifactKey))
            .andExpect(jsonPath("$.json.h").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"quality", "quality-score"})
    @DisplayName("품질 alias 요청 키가 서로 같다")
    void resolvesQualityAliases(String artifactKey) throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f34");
        Path qualityResult = tempDir.resolve("quality.json");
        Files.writeString(qualityResult, "{\"avg\":0.75}");
        Workflow workflow = new Workflow(workflowId, "p", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordQualityScoreArtifacts(5, 0.75, qualityResult.toString(), tempDir.resolve("scored.json").toString());
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/{artifactType}", workflowId, artifactKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.artifactType").value(artifactKey))
            .andExpect(jsonPath("$.json.avg").value(0.75));
    }

    @ParameterizedTest
    @ValueSource(strings = {"style", "styled"})
    @DisplayName("문체 적용 alias 요청 키가 서로 같다")
    void resolvesStyleAliases(String artifactKey) throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f35");
        Path styledPath = tempDir.resolve("styled.json");
        Files.writeString(styledPath, "{\"artifact_type\":\"style_result\"}");
        Workflow workflow = new Workflow(workflowId, "p", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordStyleArtifacts(12, styledPath.toString());
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/{artifactType}", workflowId, artifactKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.artifactType").value(artifactKey))
            .andExpect(jsonPath("$.json.artifact_type").value("style_result"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"review", "final"})
    @DisplayName("검수 결과 alias 요청 키가 서로 같다")
    void resolvesReviewAliases(String artifactKey) throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f36");
        Path finalPath = tempDir.resolve("review.json");
        Files.writeString(finalPath, "{\"review_status\":\"ok\"}");
        Workflow workflow = new Workflow(workflowId, "p", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordReviewArtifacts(0, finalPath.toString());
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/{artifactType}", workflowId, artifactKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.artifactType").value(artifactKey))
            .andExpect(jsonPath("$.json.review_status").value("ok"));
    }

    @Test
    @DisplayName("공개 번들 아티팩트를 반환한다")
    void resolvesPublicBundleKey() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f37");
        Path publicBundle = tempDir.resolve("public-bundle.json");
        Files.writeString(publicBundle, "{\"pub\":true}");
        Workflow workflow = new Workflow(workflowId, "p", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordPrivacyArtifacts(1, 0, tempDir.resolve("pr.json").toString(), publicBundle.toString());
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/public-bundle", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.artifactType").value("public-bundle"))
            .andExpect(jsonPath("$.json.pub").value(true));
    }

    @Test
    @DisplayName("스코어링된 번들 아티팩트를 반환한다")
    void resolvesScoredBundleKey() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f38");
        Path scored = tempDir.resolve("scored-bundle.json");
        Files.writeString(scored, "{\"n\":9}");
        Path qualityMain = tempDir.resolve("qm.json");
        Files.writeString(qualityMain, "{\"x\":1}");
        Workflow workflow = new Workflow(workflowId, "p", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordQualityScoreArtifacts(2, 0.9, qualityMain.toString(), scored.toString());
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/scored-bundle", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.artifactType").value("scored-bundle"))
            .andExpect(jsonPath("$.json.n").value(9));
    }

    @Test
    @DisplayName("개요 아티팩트 JSON을 반환한다")
    void fetchesOutlineArtifact() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f39");
        Path outlinePath = tempDir.resolve("outline.json");
        Files.writeString(outlinePath, "{\"outline\":{\"title\":\"T\"}}");
        Workflow workflow = new Workflow(workflowId, "p", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordOutlineArtifacts(3, outlinePath.toString());
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/outline", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.artifactType").value("outline"))
            .andExpect(jsonPath("$.json.outline.title").value("T"));
    }

    @Test
    @DisplayName("초안 아티팩트 JSON을 반환한다")
    void fetchesDraftArtifact() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f3a");
        Path draftPath = tempDir.resolve("draft.json");
        Files.writeString(draftPath, "{\"markdown\":\"# x\"}");
        Workflow workflow = new Workflow(workflowId, "p", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordDraftArtifacts(2, draftPath.toString());
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/draft", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.artifactType").value("draft"))
            .andExpect(jsonPath("$.json.markdown").value("# x"));
    }

    @Test
    @DisplayName("그룹화 아티팩트 JSON을 반환한다")
    void fetchesGroupingArtifact() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f3b");
        Path groupingPath = tempDir.resolve("grouping-result.json");
        Files.writeString(groupingPath, "{\"groups\":[{\"id\":\"g1\"}]}");
        Workflow workflow = new Workflow(workflowId, "p", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        workflow.recordGroupingArtifacts(1, groupingPath.toString());
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/artifacts/grouping", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.artifactType").value("grouping"))
            .andExpect(jsonPath("$.json.groups[0].id").value("g1"));
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
