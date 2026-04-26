package com.momently.orchestrator.adapter.in.web;

import com.momently.orchestrator.adapter.in.web.request.CreateWorkflowRequest;
import com.momently.orchestrator.adapter.in.web.response.WorkflowResponse;
import com.momently.orchestrator.application.port.in.CreateWorkflowUseCase;
import com.momently.orchestrator.application.port.in.GetWorkflowUseCase;
import com.momently.orchestrator.application.port.in.RunWorkflowUseCase;
import com.momently.orchestrator.application.port.in.command.CreateWorkflowCommand;
import com.momently.orchestrator.domain.Workflow;
import jakarta.validation.Valid;
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
 * Inbound web adapter exposing workflow APIs.
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final CreateWorkflowUseCase createWorkflowUseCase;
    private final GetWorkflowUseCase getWorkflowUseCase;
    private final RunWorkflowUseCase runWorkflowUseCase;

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
        RunWorkflowUseCase runWorkflowUseCase
    ) {
        this.createWorkflowUseCase = createWorkflowUseCase;
        this.getWorkflowUseCase = getWorkflowUseCase;
        this.runWorkflowUseCase = runWorkflowUseCase;
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

    private EntityModel<WorkflowResponse> toModel(Workflow workflow) {
        WorkflowResponse response = new WorkflowResponse(
            workflow.getWorkflowId(),
            workflow.getProjectId(),
            workflow.getGroupingStrategy(),
            workflow.getStatus(),
            workflow.getPhotoCount(),
            workflow.getGroupCount(),
            workflow.getHeroPhotoCount(),
            workflow.getPhotoInfoBundlePath(),
            workflow.getBlogPath(),
            workflow.getGroupingResultPath(),
            workflow.getHeroPhotoResultPath(),
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
