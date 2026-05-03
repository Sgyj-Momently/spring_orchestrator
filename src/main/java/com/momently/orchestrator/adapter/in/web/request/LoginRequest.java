package com.momently.orchestrator.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 콘솔 로그인 요청 본문이다.
 *
 * @param username 사용자 아이디
 * @param password 비밀번호
 */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
