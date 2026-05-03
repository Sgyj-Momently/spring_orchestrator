package com.momently.orchestrator.adapter.in.web.response;

/**
 * 발급된 접근 토큰 정보다.
 *
 * @param accessToken JWT 문자열
 * @param tokenType 보통 Bearer
 * @param expiresInSeconds 만료까지 남은 초
 */
public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
