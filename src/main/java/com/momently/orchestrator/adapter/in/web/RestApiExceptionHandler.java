package com.momently.orchestrator.adapter.in.web;

import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * REST API 공통 예외 응답이다. 작업 기록 상세 등에서 HTML 오류 페이지 대신 JSON을 내려준다.
 */
@RestControllerAdvice
public class RestApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        if (message.startsWith("Workflow not found:")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "워크플로를 찾을 수 없습니다."));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", message.isEmpty() ? "잘못된 요청입니다." : message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String field = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(FieldError::getField)
            .orElse("request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", field + " 값을 확인해 주세요."));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleIo(IOException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "파일을 읽거나 저장하지 못했습니다."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "서버가 요청을 처리할 준비가 되지 않았습니다."));
    }
}
