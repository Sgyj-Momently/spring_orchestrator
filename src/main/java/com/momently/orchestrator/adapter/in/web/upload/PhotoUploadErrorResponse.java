package com.momently.orchestrator.adapter.in.web.upload;

/**
 * 업로드 검증 실패 시 본문 메시지이다.
 *
 * @param error 사용자에게 보여줄 짧은 원인(한글)
 */
public record PhotoUploadErrorResponse(String error) {
}
