package com.momently.orchestrator.adapter.in.web;

import com.momently.orchestrator.adapter.in.web.request.CreateWorkflowRequest;
import com.momently.orchestrator.adapter.in.web.response.WorkflowArtifactResponse;
import com.momently.orchestrator.adapter.in.web.response.WorkflowResponse;
import com.momently.orchestrator.application.port.in.CreateWorkflowUseCase;
import com.momently.orchestrator.application.port.in.GetWorkflowUseCase;
import com.momently.orchestrator.application.port.in.RunWorkflowUseCase;
import com.momently.orchestrator.application.port.in.command.CreateWorkflowCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.momently.orchestrator.domain.Workflow;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 워크플로 REST API를 노출하는 웹 인바운드 어댑터
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final CreateWorkflowUseCase createWorkflowUseCase;
    private final GetWorkflowUseCase getWorkflowUseCase;
    private final RunWorkflowUseCase runWorkflowUseCase;
    private final ObjectMapper objectMapper;

    /**
     * 워크플로 컨트롤러를 생성한다.
     *
     * @param createWorkflowUseCase 워크플로 생성 유스케이스
     * @param getWorkflowUseCase 워크플로 조회 유스케이스
     * @param runWorkflowUseCase 워크플로 실행 유스케이스
     */
    public WorkflowController(
        CreateWorkflowUseCase createWorkflowUseCase,
        GetWorkflowUseCase getWorkflowUseCase,
        RunWorkflowUseCase runWorkflowUseCase,
        ObjectMapper objectMapper
    ) {
        this.createWorkflowUseCase = createWorkflowUseCase;
        this.getWorkflowUseCase = getWorkflowUseCase;
        this.runWorkflowUseCase = runWorkflowUseCase;
        this.objectMapper = objectMapper;
    }

    /**
     * 새 워크플로 리소스를 생성한다.
     *
     * @param request 워크플로 생성 요청 본문
     * @return HATEOAS 링크가 포함된 생성된 워크플로 응답
     */
    @PostMapping
    public ResponseEntity<EntityModel<WorkflowResponse>> createWorkflow(
        @Valid @RequestBody CreateWorkflowRequest request
    ) {
        Workflow workflow = createWorkflowUseCase.createWorkflow(
            new CreateWorkflowCommand(
                request.projectId(),
                request.groupingStrategy(),
                request.resolvedTimeWindowMinutes(),
                request.voiceProfileId()
            )
        );
        return ResponseEntity.ok(toModel(workflow));
    }

    /**
     * 워크플로 리소스를 조회한다.
     *
     * @param workflowId 워크플로 식별자
     * @return HATEOAS 링크가 포함된 워크플로 응답
     */
    @GetMapping("/{workflowId}")
    public ResponseEntity<EntityModel<WorkflowResponse>> getWorkflow(@PathVariable UUID workflowId) {
        Workflow workflow = getWorkflowUseCase.getWorkflow(workflowId);
        return ResponseEntity.ok(toModel(workflow));
    }

    /**
     * 워크플로 실행을 비동기로 시작하고 즉시 202 Accepted를 반환한다.
     *
     * <p>실행 결과는 Location 헤더가 가리키는 GET 엔드포인트로 상태를 폴링해 확인한다.</p>
     *
     * @param workflowId 워크플로 식별자
     * @return Location 헤더에 워크플로 상태 조회 URL이 담긴 202 Accepted
     */
    @PostMapping("/{workflowId}/run")
    public ResponseEntity<Void> runWorkflow(@PathVariable UUID workflowId) {
        runWorkflowUseCase.runWorkflow(workflowId);
        return ResponseEntity.accepted()
            .location(WebMvcLinkBuilder.linkTo(
                WebMvcLinkBuilder.methodOn(WorkflowController.class).getWorkflow(workflowId)
            ).toUri())
            .build();
    }

    /**
     * 콘솔 UI용 워크플로 아티팩트 본문을 반환한다.
     *
     * @param workflowId 워크플로 식별자
     * @param artifactType 아티팩트 유형 (예: bundle, grouping, hero, outline, draft, style, review, blog 등)
     * @return 아티팩트 내용
     */
    @GetMapping("/{workflowId}/artifacts/{artifactType}")
    public ResponseEntity<WorkflowArtifactResponse> getArtifact(
        @PathVariable UUID workflowId,
        @PathVariable String artifactType
    ) throws IOException {
        Workflow workflow = getWorkflowUseCase.getWorkflow(workflowId);
        String artifactPath = artifactPath(workflow, artifactType);
        if (artifactPath == null || artifactPath.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Path path = Path.of(artifactPath);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        if (artifactPath.endsWith(".json")) {
            JsonNode json = objectMapper.readTree(path.toFile());
            return ResponseEntity.ok(new WorkflowArtifactResponse(artifactType, artifactPath, "application/json", json, null));
        }
        String text = Files.readString(path);
        return ResponseEntity.ok(new WorkflowArtifactResponse(artifactType, artifactPath, "text/plain", null, text));
    }

    private String artifactPath(Workflow workflow, String artifactType) {
        return switch (artifactType) {
            case "bundle", "photo-info" -> workflow.getPhotoInfoBundlePath();
            case "privacy", "privacy-safety" -> workflow.getPrivacyResultPath();
            case "public-bundle" -> workflow.getPrivacyBundlePath();
            case "quality", "quality-score" -> workflow.getQualityScoreResultPath();
            case "scored-bundle" -> workflow.getQualityScoreBundlePath();
            case "blog" -> workflow.getBlogPath();
            case "grouping" -> workflow.getGroupingResultPath();
            case "hero", "hero-photo" -> workflow.getHeroPhotoResultPath();
            case "outline" -> workflow.getOutlineResultPath();
            case "draft" -> workflow.getDraftResultPath();
            case "style", "styled" -> workflow.getStyleResultPath();
            case "review", "final" -> workflow.getReviewResultPath();
            default -> null;
        };
    }

    private EntityModel<WorkflowResponse> toModel(Workflow workflow) {
        WorkflowResponse response = new WorkflowResponse(
            workflow.getWorkflowId(),
            workflow.getProjectId(),
            workflow.getGroupingStrategy(),
            workflow.getStatus(),
            workflow.getPhotoCount(),
            workflow.getPrivacyExcludedCount(),
            workflow.getAverageQualityScore(),
            workflow.getGroupCount(),
            workflow.getHeroPhotoCount(),
            workflow.getOutlineSectionCount(),
            workflow.getDraftSectionCount(),
            workflow.getStyledWordCount(),
            workflow.getReviewIssueCount(),
            workflow.getPhotoInfoBundlePath(),
            workflow.getPrivacyResultPath(),
            workflow.getPrivacyBundlePath(),
            workflow.getQualityScoreResultPath(),
            workflow.getQualityScoreBundlePath(),
            workflow.getBlogPath(),
            workflow.getGroupingResultPath(),
            workflow.getHeroPhotoResultPath(),
            workflow.getOutlineResultPath(),
            workflow.getDraftResultPath(),
            workflow.getStyleResultPath(),
            workflow.getReviewResultPath(),
            workflow.getLastFailedStep(),
            workflow.getLastErrorMessage()
        );
        return EntityModel.of(
            response,
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WorkflowController.class)
                .getWorkflow(workflow.getWorkflowId()))
                .withSelfRel(),
            WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(WorkflowController.class)
                .runWorkflow(workflow.getWorkflowId()))
                .withRel("run")
        );
    }
}
