package com.momently.orchestrator.adapter.in.web;

import com.momently.orchestrator.adapter.in.web.request.CreateWorkflowRequest;
import com.momently.orchestrator.adapter.in.web.request.RestyleRequest;
import com.momently.orchestrator.adapter.in.web.response.RestyleResponse;
import com.momently.orchestrator.adapter.in.web.response.WorkflowArtifactResponse;
import com.momently.orchestrator.adapter.in.web.response.WorkflowResponse;
import com.momently.orchestrator.application.port.in.CreateWorkflowUseCase;
import com.momently.orchestrator.application.port.in.GetWorkflowUseCase;
import com.momently.orchestrator.application.port.in.RunWorkflowUseCase;
import com.momently.orchestrator.application.port.in.command.CreateWorkflowCommand;
import com.momently.orchestrator.application.port.out.ReviewAgentPort;
import com.momently.orchestrator.application.port.out.StyleAgentPort;
import com.momently.orchestrator.application.port.out.result.DraftResult;
import com.momently.orchestrator.application.port.out.result.PhotoInfoResult;
import com.momently.orchestrator.application.port.out.result.ReviewResult;
import com.momently.orchestrator.application.port.out.result.StyleResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.momently.orchestrator.application.port.out.WorkflowRepository;
import com.momently.orchestrator.config.PhotoInfoPipelineProperties;
import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final PhotoInfoPipelineProperties photoInfoPipelineProperties;
    private final StyleAgentPort styleAgentPort;
    private final ReviewAgentPort reviewAgentPort;
    private final WorkflowRepository workflowRepository;

    /**
     * 워크플로 컨트롤러를 생성한다.
     *
     * @param createWorkflowUseCase 워크플로 생성 유스케이스
     * @param getWorkflowUseCase 워크플로 조회 유스케이스
     * @param runWorkflowUseCase 워크플로 실행 유스케이스
     * @param styleAgentPort 문체 적용 에이전트 포트
     * @param reviewAgentPort 검수 에이전트 포트
     * @param workflowRepository 워크플로 저장 포트
     */
    public WorkflowController(
        CreateWorkflowUseCase createWorkflowUseCase,
        GetWorkflowUseCase getWorkflowUseCase,
        RunWorkflowUseCase runWorkflowUseCase,
        ObjectMapper objectMapper,
        PhotoInfoPipelineProperties photoInfoPipelineProperties,
        StyleAgentPort styleAgentPort,
        ReviewAgentPort reviewAgentPort,
        WorkflowRepository workflowRepository
    ) {
        this.createWorkflowUseCase = createWorkflowUseCase;
        this.getWorkflowUseCase = getWorkflowUseCase;
        this.runWorkflowUseCase = runWorkflowUseCase;
        this.objectMapper = objectMapper;
        this.photoInfoPipelineProperties = photoInfoPipelineProperties;
        this.styleAgentPort = styleAgentPort;
        this.reviewAgentPort = reviewAgentPort;
        this.workflowRepository = workflowRepository;
    }

    /**
     * 저장된 워크플로 목록을 반환한다.
     */
    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> listWorkflows() {
        return ResponseEntity.ok(getWorkflowUseCase.listWorkflows().stream()
            .map(this::toResponse)
            .toList());
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
     * 콘솔 작업 기록을 모두 삭제한다.
     *
     * <p>워크플로 메타데이터만 삭제하며, 이미 생성된 산출물 파일은 건드리지 않는다.</p>
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteWorkflowHistory() {
        getWorkflowUseCase.deleteAllWorkflows();
        return ResponseEntity.noContent().build();
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
     * 완료된 워크플로의 문체를 재적용하고 검수를 다시 수행한다.
     *
     * <p>기존 draft 아티팩트를 기반으로 style agent → review agent 순서로 재실행한다.
     * voiceProfileId가 지정되면 해당 프로필로, 지정하지 않으면 워크플로 원래 프로필로 적용한다.</p>
     *
     * @param workflowId 워크플로 식별자
     * @param request restyle 요청 본문
     * @return 201 Created with 최종 마크다운, style 상태, review 상태
     */
    @PostMapping("/{workflowId}/restyle")
    public ResponseEntity<Map<String, String>> restyleWorkflow(
        @PathVariable UUID workflowId,
        @RequestBody(required = false) RestyleRequest request
    ) {
        Workflow workflow = getWorkflowUseCase.getWorkflow(workflowId);
        if (workflow.getStatus() != WorkflowStatus.COMPLETED) {
            return ResponseEntity.badRequest().build();
        }
        String draftResultPath = workflow.getDraftResultPath();
        if (draftResultPath == null || draftResultPath.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String effectiveVoiceProfileId = request != null && request.voiceProfileId() != null
            ? request.voiceProfileId()
            : workflow.getVoiceProfileId();
        String extraInstructions = request != null ? request.extraInstructions() : null;

        // 비동기 실행 — LLM 호출이 수 분 걸릴 수 있으므로 즉시 202 반환 후 백그라운드에서 처리
        CompletableFuture.runAsync(() -> executeRestyle(workflow, effectiveVoiceProfileId, extraInstructions));

        return ResponseEntity.accepted()
            .body(Map.of("status", "processing", "workflowId", workflowId.toString()));
    }

    private void executeRestyle(Workflow workflow, String voiceProfileId, String extraInstructions) {
        try {
            int draftSectionCount = workflow.getDraftSectionCount() == null ? 0 : workflow.getDraftSectionCount();
            DraftResult draftResult = new DraftResult(draftSectionCount, workflow.getDraftResultPath());

            StyleResult styleResult = styleAgentPort.applyStyle(
                workflow.getProjectId(), draftResult, voiceProfileId
            );
            workflow.recordStyleArtifacts(styleResult.wordCount(), styleResult.resultPath());

            String bundlePath = workflow.getQualityScoreBundlePath() != null
                ? workflow.getQualityScoreBundlePath()
                : workflow.getPrivacyBundlePath() != null
                    ? workflow.getPrivacyBundlePath()
                    : workflow.getPhotoInfoBundlePath();
            int photoCount = workflow.getPhotoCount() == null ? 0 : workflow.getPhotoCount();
            PhotoInfoResult photoInfoResult = new PhotoInfoResult(photoCount, bundlePath, workflow.getBlogPath());

            ReviewResult reviewResult = reviewAgentPort.reviewDocument(
                workflow.getProjectId(), photoInfoResult, styleResult
            );
            workflow.recordReviewArtifacts(reviewResult.issueCount(), reviewResult.resultPath());
            workflowRepository.save(workflow);
        } catch (Exception e) {
            // 로그만 남기고 조용히 종료 (클라이언트는 폴링으로 결과 확인)
            System.err.println("[restyle] 실패: " + e.getMessage());
        }
    }

    private String readJsonField(Path path, String fieldName) {
        try {
            if (!Files.exists(path)) {
                return null;
            }
            JsonNode root = objectMapper.readTree(path.toFile());
            JsonNode field = root.get(fieldName);
            return field != null && !field.isNull() ? field.asText() : null;
        } catch (IOException exception) {
            return null;
        }
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

    /**
     * Markdown 미리보기에서 참조하는 프로젝트 원본 이미지를 내려준다.
     *
     * <p>파일명만 허용하고, 실제 경로는 서버 설정의 input-root/projectId 아래로만 제한한다.</p>
     */
    @GetMapping("/{workflowId}/files/{fileName:.+}")
    public ResponseEntity<Resource> getProjectFile(
        @PathVariable UUID workflowId,
        @PathVariable String fileName
    ) throws MalformedURLException {
        Workflow workflow = getWorkflowUseCase.getWorkflow(workflowId);
        if (fileName == null
            || fileName.isBlank()
            || fileName.contains("/")
            || fileName.contains("\\")
            || fileName.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        Path projectRoot = Path.of(photoInfoPipelineProperties.inputRoot())
            .toAbsolutePath()
            .normalize()
            .resolve(workflow.getProjectId())
            .normalize();
        Path file = projectRoot.resolve(fileName).normalize();
        if (!file.startsWith(projectRoot) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(file.toUri());
        return ResponseEntity.ok()
            .contentType(mediaType(fileName))
            .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
            .body(resource);
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
        WorkflowResponse response = toResponse(workflow);
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

    private WorkflowResponse toResponse(Workflow workflow) {
        return new WorkflowResponse(
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
    }

    private static MediaType mediaType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".heic")) {
            return MediaType.parseMediaType("image/heic");
        }
        if (lower.endsWith(".heif")) {
            return MediaType.parseMediaType("image/heif");
        }
        if (lower.endsWith(".mp4") || lower.endsWith(".m4v")) {
            return MediaType.parseMediaType("video/mp4");
        }
        if (lower.endsWith(".mov")) {
            return MediaType.parseMediaType("video/quicktime");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
