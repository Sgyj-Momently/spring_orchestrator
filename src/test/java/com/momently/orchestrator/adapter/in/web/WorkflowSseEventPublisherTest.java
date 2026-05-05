package com.momently.orchestrator.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.momently.orchestrator.domain.Workflow;
import com.momently.orchestrator.domain.WorkflowStatus;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class WorkflowSseEventPublisherTest {

    @Test
    @DisplayName("구독자가 없어도 이벤트 발행은 조용히 무시한다")
    void ignoresPublishWithoutSubscribers() {
        WorkflowSseEventPublisher publisher = new WorkflowSseEventPublisher();
        Workflow workflow = workflow();

        assertThatCode(() -> publisher.publish(workflow)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("구독 직후 emitter를 반환하고 이후 이벤트를 발행할 수 있다")
    void subscribesAndPublishesWorkflowEvents() {
        WorkflowSseEventPublisher publisher = new WorkflowSseEventPublisher();
        Workflow workflow = workflow();

        SseEmitter emitter = publisher.subscribe(workflow);

        assertThatCode(() -> publisher.publish(workflow)).doesNotThrowAnyException();
        emitter.complete();
        assertThatCode(() -> publisher.publish(workflow)).doesNotThrowAnyException();
    }

    private Workflow workflow() {
        return new Workflow(
            UUID.fromString("01964e72-4f4b-7d35-9a07-f9c7ef4b0f43"),
            "project-sse",
            "TIME_BASED",
            90,
            WorkflowStatus.CREATED
        );
    }
}
