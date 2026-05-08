package com.momently.orchestrator.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RestApiExceptionHandlerTest {

    private final RestApiExceptionHandler handler = new RestApiExceptionHandler();

    @Test
    @DisplayName("파일 IO 예외는 사용자용 JSON 500으로 매핑한다")
    void mapsIoException() {
        var response = handler.handleIo(new IOException("disk"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "파일을 읽거나 저장하지 못했습니다.");
    }

    @Test
    @DisplayName("서버 준비 상태 예외는 사용자용 JSON 503으로 매핑한다")
    void mapsIllegalStateException() {
        var response = handler.handleIllegalState(new IllegalStateException("not ready"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("error", "서버가 요청을 처리할 준비가 되지 않았습니다.");
    }
}
