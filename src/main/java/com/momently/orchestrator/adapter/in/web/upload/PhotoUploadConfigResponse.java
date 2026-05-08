package com.momently.orchestrator.adapter.in.web.upload;

import java.util.List;

/**
 * 콘솔이 서버 업로드 제한과 지원 확장자를 화면 검증에 맞춰 쓸 수 있게 내려주는 응답이다.
 */
public record PhotoUploadConfigResponse(
    int maxFiles,
    long maxBytesPerFile,
    long maxTotalBytes,
    List<String> allowedExtensions
) {
}
