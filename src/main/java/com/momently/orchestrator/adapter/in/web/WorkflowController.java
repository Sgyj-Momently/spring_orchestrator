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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound web adapter exposing workflow APIs.
 */
@RestController
@RequestMapping("/api/v1/workflows")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class WorkflowController {

    private final CreateWorkflowUseCase createWorkflowUseCase;
    private final GetWorkflowUseCase getWorkflowUseCase;
    private final RunWorkflowUseCase runWorkflowUseCase;
    private final ObjectMapper objectMapper;

    /**
     * Creates the workflow controller.
     *
     * @param createWorkflowUseCase workflow creation use case
     * @param getWorkflowUseCase workflow lookup use case
     * @param runWorkflowUseCase workflow execution use case
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
     * Creates a new workflow resource.
     *
     * @param request workflow creation request
     * @return created workflow response with HATEOAS links
     */
    @PostMapping
    public ResponseEntity<EntityModel<WorkflowResponse>> createWorkflow(
        @Valid @RequestBody CreateWorkflowRequest request
    ) {
        Workflow workflow = createWorkflowUseCase.createWorkflow(
            new CreateWorkflowCommand(request.projectId(), request.groupingStrategy(), request.resolvedTimeWindowMinutes())
        );
        return ResponseEntity.ok(toModel(workflow));
    }

    /**
     * Retrieves a workflow resource.
     *
     * @param workflowId workflow identifier
     * @return workflow response with HATEOAS links
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
     * @param workflowId workflow identifier
     * @return 202 Accepted with Location header pointing to the workflow status endpoint
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
     * Returns a workflow artifact content for the console UI.
     *
     * @param workflowId workflow identifier
     * @param artifactType artifact type such as bundle, grouping, hero, outline, draft, style, review, or blog
     * @return artifact content
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
            workflow.getGroupCount(),
            workflow.getHeroPhotoCount(),
            workflow.getOutlineSectionCount(),
            workflow.getDraftSectionCount(),
            workflow.getStyledWordCount(),
            workflow.getReviewIssueCount(),
            workflow.getPhotoInfoBundlePath(),
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
