package com.momently.orchestrator.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 콘솔 회원가입 요청 본문이다.
 *
 * @param username 사용자 아이디
 * @param password 비밀번호
 * @param inviteCode 초대 코드
 */
public record RegisterRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 8, max = 120) String password,
    @NotBlank String inviteCode
) {
}
