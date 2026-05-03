package com.momently.orchestrator.adapter.in.web.response;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 콘솔 UI용 워크플로 단일 아티팩트 응답 본문이다.
 *
 * @param artifactType 클라이언트가 요청한 아티팩트 종류 문자열
 * @param path 서버 디스크상 아티팩트 파일 경로
 * @param contentType 본문 MIME 유형 (JSON 또는 평문)
 * @param json JSON 아티팩트일 때 파싱된 트리, 아니면 null
 * @param text 평문 아티팩트일 때 본문, 아니면 null
 */
public record WorkflowArtifactResponse(
    String artifactType,
    String path,
    String contentType,
    JsonNode json,
    String text
) {
}

