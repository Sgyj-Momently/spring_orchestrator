package com.momently.orchestrator.adapter.in.web.request;

/**
 * 문체 재적용(restyle) 요청 본문이다.
 *
 * @param voiceProfileId 말투 프로필 식별자, null이면 기존 워크플로 설정 유지
 * @param extraInstructions 추가 지시문, null이면 없음
 */
public record RestyleRequest(
    String voiceProfileId,
    String extraInstructions
) {
}
