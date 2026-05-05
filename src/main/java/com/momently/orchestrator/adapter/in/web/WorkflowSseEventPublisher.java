package com.momently.orchestrator.adapter.in.web;

import com.momently.orchestrator.application.port.out.WorkflowEventPort;
import com.momently.orchestrator.domain.Workflow;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 워크플로 상태 변경을 Server-Sent Events로 브라우저에 전달한다.
 */
@Component
public class WorkflowSseEventPublisher implements WorkflowEventPort {

    private static final long EMITTER_TIMEOUT_MILLIS = 30L * 60L * 1000L;
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * 특정 워크플로 상태 이벤트를 구독한다.
     *
     * @param workflow 현재 워크플로. 구독 직후 snapshot 이벤트로 즉시 전송한다.
     * @return SSE emitter
     */
    public SseEmitter subscribe(Workflow workflow) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        UUID workflowId = workflow.getWorkflowId();
        emitters.computeIfAbsent(workflowId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(workflowId, emitter));
        emitter.onTimeout(() -> remove(workflowId, emitter));
        emitter.onError(_ -> remove(workflowId, emitter));
        send(emitter, workflow);
        return emitter;
    }

    @Override
    public void publish(Workflow workflow) {
        List<SseEmitter> workflowEmitters = emitters.get(workflow.getWorkflowId());
        if (workflowEmitters == null || workflowEmitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : workflowEmitters) {
            send(emitter, workflow);
        }
    }

    private void send(SseEmitter emitter, Workflow workflow) {
        try {
            emitter.send(SseEmitter.event()
                .name("workflow")
                .id(workflow.getStatus().name())
                .data(WorkflowEventPayload.from(workflow)));
        } catch (IOException | IllegalStateException exception) {
            remove(workflow.getWorkflowId(), emitter);
        }
    }

    private void remove(UUID workflowId, SseEmitter emitter) {
        List<SseEmitter> workflowEmitters = emitters.get(workflowId);
        if (workflowEmitters == null) {
            return;
        }
        workflowEmitters.remove(emitter);
        if (workflowEmitters.isEmpty()) {
            emitters.remove(workflowId);
        }
    }
}
