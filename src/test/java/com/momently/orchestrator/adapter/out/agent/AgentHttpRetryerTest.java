package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momently.orchestrator.config.AgentHttpClientProperties;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * 에이전트 HTTP 재시도 정책을 검증한다.
 */
class AgentHttpRetryerTest {

    @Test
    @DisplayName("일시적인 5xx 응답은 설정된 횟수 안에서 재시도한다")
    void retriesServerError() {
        AgentHttpRetryer retryer = new AgentHttpRetryer(new AgentHttpClientProperties(5, 300, 2, 0));
        AtomicInteger calls = new AtomicInteger();

        String result = retryer.execute(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new HttpServerErrorException(HttpStatusCode.valueOf(503), "unavailable");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(2);
    }

    @Test
    @DisplayName("네트워크 접근 오류도 재시도 대상이다")
    void retriesResourceAccessError() {
        AgentHttpRetryer retryer = new AgentHttpRetryer(new AgentHttpClientProperties(5, 300, 2, 0));
        AtomicInteger calls = new AtomicInteger();

        String result = retryer.execute(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new ResourceAccessException("timeout", new SocketTimeoutException("read timed out"));
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(2);
    }

    @Test
    @DisplayName("최대 시도 후에도 실패하면 마지막 예외를 유지한다")
    void throwsLastFailureAfterMaxAttempts() {
        AgentHttpRetryer retryer = new AgentHttpRetryer(new AgentHttpClientProperties(5, 300, 2, 0));
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> retryer.execute(() -> {
            calls.incrementAndGet();
            throw new HttpServerErrorException(HttpStatusCode.valueOf(502), "bad gateway");
        }))
            .isInstanceOf(HttpServerErrorException.class);

        assertThat(calls).hasValue(2);
    }
}
