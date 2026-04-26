package com.momently.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 로컬 사진 정보 추출 CLI 파이프라인 실행에 필요한 내부 설정이다.
 *
 * <p>사진 정보 추출 모듈은 아직 HTTP 서비스가 아니라 {@code run_pipeline.py} CLI로 제공된다.
 * 따라서 Spring 오케스트레이터는 이 설정을 통해 실행 파일, 입력/출력 루트, 모델 값을 내부적으로
 * 조립한다. 이 값들은 공개 API 계약에 노출하지 않고 운영 설정으로만 변경한다.</p>
 *
 * @param pythonExecutable 로컬 파이프라인을 실행할 Python 명령 또는 절대 경로
 * @param scriptPath 사진 정보 추출 진입점인 {@code run_pipeline.py} 경로
 * @param inputRoot 프로젝트 식별자별 사진 폴더를 찾을 상위 디렉터리
 * @param outputRoot 프로젝트 식별자별 EXIF, summary, bundle 산출물을 쓸 상위 디렉터리
 * @param ollamaBaseUrl 사진 분석과 bundle 요약에 사용할 Ollama 서버 주소
 * @param visionModel 사진별 이미지 분석에 사용할 비전 모델명
 * @param writerModel bundle 요약에 사용할 텍스트 모델명
 * @param ollamaTimeoutSeconds Ollama 호출이 멈췄을 때 실패로 전환할 타임아웃 초
 * @param skipBlog 오케스트레이션 실행에서 선택적 블로그 생성 단계를 건너뛸지 여부
 * @param force 기존 캐시(EXIF/photo summary)를 무시하고 재생성할지 여부
 */
@ConfigurationProperties(prefix = "agents.photo-info.pipeline")
public record PhotoInfoPipelineProperties(
    String pythonExecutable,
    String scriptPath,
    String inputRoot,
    String outputRoot,
    String ollamaBaseUrl,
    String visionModel,
    String writerModel,
    int ollamaTimeoutSeconds,
    boolean skipBlog,
    boolean force
) {

    /**
     * 비어 있는 설정값에 로컬 개발 기본값을 채운다.
     *
     * <p>테스트와 로컬 실행에서 설정을 모두 적지 않아도 Spring context가 뜰 수 있게 하기 위한
     * 방어적 기본값이다. 운영 환경에서는 배포 경로와 모델명을 명시적으로 덮어쓰는 것을 권장한다.</p>
     */
    public PhotoInfoPipelineProperties {
        pythonExecutable = defaultIfBlank(pythonExecutable, "python3");
        scriptPath = defaultIfBlank(scriptPath, "../photo_exif_llm_pipeline/src/run_pipeline.py");
        inputRoot = defaultIfBlank(inputRoot, "../photo_exif_llm_pipeline/input_photos");
        outputRoot = defaultIfBlank(outputRoot, "../photo_exif_llm_pipeline/output/orchestrator");
        ollamaBaseUrl = defaultIfBlank(ollamaBaseUrl, "http://localhost:11434");
        visionModel = defaultIfBlank(visionModel, "qwen2.5vl:7b");
        writerModel = defaultIfBlank(writerModel, "qwen2.5:14b");
        if (ollamaTimeoutSeconds <= 0) {
            ollamaTimeoutSeconds = 180;
        }
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
