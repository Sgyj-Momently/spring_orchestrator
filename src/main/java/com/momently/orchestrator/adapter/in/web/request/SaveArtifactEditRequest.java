package com.momently.orchestrator.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 콘솔에서 사용자가 편집한 마크다운 산출물을 서버에 저장하는 요청이다.
 */
public record SaveArtifactEditRequest(
    @NotBlank
    @Size(max = 500_000)
    String markdown
) {
}
