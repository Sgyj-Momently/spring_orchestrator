package com.momently.orchestrator.adapter.out.agent;

import com.momently.orchestrator.config.AgentHttpClientProperties;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * FastAPI 에이전트 호출의 짧은 일시 장애를 흡수하는 공통 재시도기다.
 */
@Component
public class AgentHttpRetryer {

    private final AgentHttpClientProperties properties;

    public AgentHttpRetryer(AgentHttpClientProperties properties) {
        this.properties = properties;
    }

    public <T> T execute(Supplier<T> call) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                return call.get();
            } catch (RestClientResponseException exception) {
                lastFailure = exception;
                if (!isRetryableStatus(exception) || attempt >= properties.maxAttempts()) {
                    throw exception;
                }
                pause();
            } catch (ResourceAccessException exception) {
                lastFailure = exception;
                if (attempt >= properties.maxAttempts()) {
                    throw exception;
                }
                pause();
            }
        }
        throw lastFailure == null ? new IllegalStateException("Agent HTTP retry failed without an exception") : lastFailure;
    }

    private boolean isRetryableStatus(RestClientResponseException exception) {
        return exception.getStatusCode().is5xxServerError()
            || exception.getStatusCode().value() == 429;
    }

    private void pause() {
        if (properties.backoffMillis() <= 0) {
            return;
        }
        try {
            Thread.sleep(properties.backoffMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry agent HTTP call", exception);
        }
    }
}
