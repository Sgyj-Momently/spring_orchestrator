package com.momently.orchestrator.adapter.in.web.response;

/**
 * 문체 재적용(restyle) 응답 본문이다.
 *
 * @param finalMarkdown 최종 검수 완료된 마크다운
 * @param styleStatus 문체 적용 에이전트의 상태
 * @param reviewStatus 검수 에이전트의 상태
 */
public record RestyleResponse(
    String finalMarkdown,
    String styleStatus,
    String reviewStatus
) {
}
