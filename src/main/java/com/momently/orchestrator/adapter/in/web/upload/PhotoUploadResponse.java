package com.momently.orchestrator.adapter.in.web.upload;

/**
 * 미디어 업로드가 끝나면 워크플로 생성에 쓸 프로젝트 식별자를 돌려준다.
 *
 * @param projectId 파이프라인 입력 폴더명(서버 발급)
 * @param savedCount 저장된 미디어 파일 수
 * @param bytesTotal 저장된 총 바이트 수
 */
public record PhotoUploadResponse(String projectId, int savedCount, long bytesTotal) {
}
