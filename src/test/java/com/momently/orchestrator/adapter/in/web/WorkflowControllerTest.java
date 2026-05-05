package com.momently.orchestrator.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.momently.orchestrator.application.port.in.CreateWorkflowUseCase;
import com.momently.orchestrator.application.port.in.GetWorkflowUseCase;
import com.momently.orchestrator.application.port.in.RunWorkflowUseCase;
import com.momently.orchestrator.application.port.out.ReviewAgentPort;
import com.momently.orchestrator.application.port.out.StyleAgentPort;
import com.momently.orchestrator.application.port.out.WorkflowRepository;
import com.momently.orchestrator.application.port.out.result.ReviewResult;
import com.momently.orchestrator.application.port.out.result.StyleResult;
import com.momently.orchestrator.config.PhotoInfoPipelineProperties;
import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.application.service.WorkflowStateMachine;
import com.momently.orchestrator.security.JwtService;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.List;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 워크플로 웹 어댑터의 요청/응답 계약을 검증한다.
 */
@WebMvcTest(WorkflowController.class)
@Import(RestApiExceptionHandler.class)
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

    @MockBean
    private PhotoInfoPipelineProperties photoInfoPipelineProperties;

    @MockBean
    private StyleAgentPort styleAgentPort;

    @MockBean
    private ReviewAgentPort reviewAgentPort;

    @MockBean
    private WorkflowRepository workflowRepository;

    @MockBean
    private WorkflowSseEventPublisher workflowSseEventPublisher;

    @MockBean
    private WorkflowStateMachine workflowStateMachine;

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
    @DisplayName("없는 워크플로 조회 시 404 JSON")
    void getMissingWorkflowReturns404() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f99");
        when(getWorkflowUseCase.getWorkflow(workflowId))
            .thenThrow(new IllegalArgumentException("Workflow not found: " + workflowId));

        mockMvc.perform(get("/api/v1/workflows/{workflowId}", workflowId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("워크플로를 찾을 수 없습니다."));
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
    @DisplayName("워크플로 상태 이벤트 SSE 스트림을 연다")
    void subscribesWorkflowEvents() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f39");
        Workflow workflow = new Workflow(workflowId, "project-events", "TIME_BASED", 90, WorkflowStatus.CREATED);
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
            new org.springframework.web.servlet.mvc.method.annotation.SseEmitter();
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);
        when(workflowSseEventPublisher.subscribe(workflow)).thenReturn(emitter);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/events", workflowId))
            .andExpect(status().isOk());

        verify(workflowSseEventPublisher).subscribe(workflow);
        emitter.complete();
    }

    @Test
    @DisplayName("워크플로 목록을 서버 기록으로 조회한다")
    void listsWorkflows() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f19");
        Workflow workflow = new Workflow(
            workflowId,
            "project-history",
            "LOCATION_BASED",
            90,
            WorkflowStatus.COMPLETED
        );
        when(getWorkflowUseCase.listWorkflows()).thenReturn(List.of(workflow));

        mockMvc.perform(get("/api/v1/workflows"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].workflowId").value(workflowId.toString()))
            .andExpect(jsonPath("$[0].projectId").value("project-history"))
            .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("워크플로 기록을 모두 삭제한다")
    void deletesWorkflowHistory() throws Exception {
        mockMvc.perform(delete("/api/v1/workflows"))
            .andExpect(status().isNoContent());

        verify(getWorkflowUseCase).deleteAllWorkflows();
    }

    @Test
    @DisplayName("워크플로 실행 요청을 받아 202 Accepted와 상태 조회용 Location 헤더를 반환한다")
    void runsWorkflow() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f12");
        Workflow workflow = new Workflow(workflowId, "project-run", "TIME_BASED", 90, WorkflowStatus.CREATED);
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(post("/api/v1/workflows/{workflowId}/run", workflowId))
            .andExpect(status().isAccepted())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.status").value("started"));

        verify(runWorkflowUseCase).runWorkflow(workflowId);
    }

    @Test
    @DisplayName("실행 중인 워크플로 실행 요청은 중복 실행하지 않고 멱등 응답한다")
    void runWorkflowIsIdempotentWhileRunning() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f42");
        Workflow workflow = new Workflow(workflowId, "project-running", "TIME_BASED", 90, WorkflowStatus.DRAFT_CREATING);
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(post("/api/v1/workflows/{workflowId}/run", workflowId))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("already_running"));

        verify(runWorkflowUseCase, never()).runWorkflow(workflowId);
    }

    @Test
    @DisplayName("실패한 워크플로는 retry 엔드포인트로 재실행한다")
    void retriesFailedWorkflow() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f43");
        Workflow workflow = new Workflow(workflowId, "project-retry", "TIME_BASED", 90, WorkflowStatus.FAILED);
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(post("/api/v1/workflows/{workflowId}/retry", workflowId))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("retry_started"));

        verify(runWorkflowUseCase).runWorkflow(workflowId);
    }

    @Test
    @DisplayName("실패 상태가 아닌 워크플로 retry 요청은 409로 거절한다")
    void rejectsRetryForNonFailedWorkflow() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f44");
        Workflow workflow = new Workflow(workflowId, "project-retry", "TIME_BASED", 90, WorkflowStatus.CREATED);
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(post("/api/v1/workflows/{workflowId}/retry", workflowId))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("not_retryable"));

        verify(runWorkflowUseCase, never()).runWorkflow(workflowId);
    }

    @Test
    @DisplayName("완료된 워크플로는 기존 초안으로 문체를 다시 적용하고 검수한다")
    void restylesCompletedWorkflow() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f38");
        Path draftPath = tempDir.resolve("draft.json");
        Path stylePath = tempDir.resolve("style.json");
        Path reviewPath = tempDir.resolve("review.json");
        Files.writeString(draftPath, "{\"section_count\":2}");
        Files.writeString(stylePath, "{\"style_status\":\"ok\"}");
        Files.writeString(reviewPath, "{\"review_status\":\"ok\",\"final_markdown\":\"# 다시 쓴 글\"}");
        Workflow workflow = new Workflow(workflowId, "project-restyle", "TIME_BASED", 90, "preset_a", WorkflowStatus.COMPLETED);
        workflow.recordPhotoInfoArtifacts(3, tempDir.resolve("bundle.json").toString(), null);
        workflow.recordDraftArtifacts(2, draftPath.toString());
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);
        when(styleAgentPort.applyStyle(any(), any(), any())).thenReturn(new StyleResult(120, stylePath.toString()));
        when(reviewAgentPort.reviewDocument(any(), any(), any())).thenReturn(new ReviewResult(0, reviewPath.toString()));

        mockMvc.perform(post("/api/v1/workflows/{workflowId}/restyle", workflowId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"voiceProfileId\":\"preset_b\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("processing"))
            .andExpect(jsonPath("$.workflowId").value(workflowId.toString()));

        verify(workflowRepository, timeout(1000).atLeastOnce()).save(workflow);
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
    @DisplayName("워크플로 프로젝트 이미지 파일을 내려준다")
    void downloadsProjectImageFile() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f17");
        Path projectDir = Files.createDirectories(tempDir.resolve("project-007"));
        Path image = projectDir.resolve("IMG_0001.jpg");
        Files.write(image, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff});
        Workflow workflow = new Workflow(workflowId, "project-007", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);
        when(photoInfoPipelineProperties.inputRoot()).thenReturn(tempDir.toString());

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/files/{fileName}", workflowId, "IMG_0001.jpg"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/jpeg"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"clip.mp4:video/mp4", "clip.m4v:video/mp4", "clip.mov:video/quicktime"})
    @DisplayName("워크플로 프로젝트 동영상 파일은 동영상 Content-Type으로 내려준다")
    void downloadsProjectVideoFile(String fileAndType) throws Exception {
        String[] parts = fileAndType.split(":");
        String fileName = parts[0];
        String contentType = parts[1];
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f37");
        Path projectDir = Files.createDirectories(tempDir.resolve("project-video"));
        Files.write(projectDir.resolve(fileName), new byte[] {0, 0, 0, 24, 'f', 't', 'y', 'p'});
        Workflow workflow = new Workflow(workflowId, "project-video", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);
        when(photoInfoPipelineProperties.inputRoot()).thenReturn(tempDir.toString());

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/files/{fileName}", workflowId, fileName))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", contentType));
    }

    @Test
    @DisplayName("프로젝트 이미지 다운로드는 경로 이동을 거절한다")
    void rejectsUnsafeProjectFileName() throws Exception {
        UUID workflowId = UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f18");
        Workflow workflow = new Workflow(workflowId, "project-008", "TIME_BASED", 90, WorkflowStatus.COMPLETED);
        when(getWorkflowUseCase.getWorkflow(workflowId)).thenReturn(workflow);

        mockMvc.perform(get("/api/v1/workflows/{workflowId}/files/{fileName}", workflowId, "..%2Fsecret.jpg"))
            .andExpect(status().isBadRequest());
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
